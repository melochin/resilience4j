/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.MaxRetriesExceeded;
import io.github.resilience4j.retry.MaxRetriesExceededException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.*;
import io.vavr.CheckedConsumer;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryImpl<T> implements Retry {


    /*package*/ static CheckedConsumer<Long> sleepFunction = Thread::sleep;
    private final Metrics metrics;
    private final RetryEventProcessor eventProcessor;
    @Nullable
    private final Predicate<T> resultPredicate;
    private final String name;
    private final RetryConfig config;
    private final Map<String, String> tags;

    private final int maxAttempts;
    private final boolean failAfterMaxAttempts;
    private final IntervalBiFunction<T> intervalBiFunction;
    private final Predicate<Throwable> exceptionPredicate;
    private final LongAdder succeededAfterRetryCounter;
    private final LongAdder failedAfterRetryCounter;
    private final LongAdder succeededWithoutRetryCounter;
    private final LongAdder failedWithoutRetryCounter;

    public RetryImpl(String name, RetryConfig config) {
        this(name, config, HashMap.empty());
    }

    public RetryImpl(String name, RetryConfig config, Map<String, String> tags) {
        this.name = name;
        this.config = config;
        this.tags = tags;
        this.maxAttempts = config.getMaxAttempts();
        this.failAfterMaxAttempts = config.isFailAfterMaxAttempts();
        this.intervalBiFunction = config.getIntervalBiFunction();
        this.exceptionPredicate = config.getExceptionPredicate();
        this.resultPredicate = config.getResultPredicate();
        this.metrics = this.new RetryMetrics();
        this.eventProcessor = new RetryEventProcessor();
        succeededAfterRetryCounter = new LongAdder();
        failedAfterRetryCounter = new LongAdder();
        succeededWithoutRetryCounter = new LongAdder();
        failedWithoutRetryCounter = new LongAdder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Context context() {
        return new ContextImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AsyncContext asyncContext() {
        return new AsyncContextImpl();
    }

    @Override
    public RetryConfig getRetryConfig() {
        return config;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    private void publishRetryEvent(Supplier<RetryEvent> event) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(event.get());
        }
    }

    @Override
    public EventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metrics getMetrics() {
        return this.metrics;
    }

    public final class ContextImpl implements Retry.Context<T> {
        // 尝试次数
        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        // 上次异常
        private final AtomicReference<Exception> lastException = new AtomicReference<>();
        // 上次运行时异常
        private final AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

        private ContextImpl() {
        }

        /**
         * @deprecated since 1.2.0
         */
        @Override
        @Deprecated
        public void onSuccess() {
            onComplete();
        }

        @Override
        public void onComplete() {
            // 获得当前尝试次数
            int currentNumOfAttempts = numOfAttempts.get();
            // 如果有一定尝试次数 且 没有超过上限
            if (currentNumOfAttempts > 0 && currentNumOfAttempts < maxAttempts) {
                // 统计尝试后成功执行次数
                succeededAfterRetryCounter.increment();
                // 如果最近有一次异常
                Throwable throwable = Option.of(lastException.get())
                    .getOrElse(lastRuntimeException.get());
                // 发布事件 - retry name, 总共尝试次数， 最近一次异常
                publishRetryEvent(
                    () -> new RetryOnSuccessEvent(getName(), currentNumOfAttempts, throwable));
            } else {
                // 如果超过上限
                if (currentNumOfAttempts >= maxAttempts) {
                    // 统计尝试后的失败执行次数
                    failedAfterRetryCounter.increment();
                    Throwable throwable = Option.of(lastException.get())
                        .orElse(Option.of(lastRuntimeException.get()))
                        .filter(p -> !failAfterMaxAttempts)
                        .getOrElse(new MaxRetriesExceeded(
                            "max retries is reached out for the result predicate check"
                        ));
                    publishRetryEvent(() -> new RetryOnErrorEvent(name, currentNumOfAttempts, throwable));
                    // 达到最大上限
                    if (failAfterMaxAttempts) {
                        throw MaxRetriesExceededException.createMaxRetriesExceededException(RetryImpl.this);
                    }
                } else {
                    // 没有超过上限，并且没有尝试过 （一次过）
                    // 统计没有重试的成功次数
                    succeededWithoutRetryCounter.increment();
                }
            }
        }

        @Override
        public boolean onResult(T result) {
            // 根据函数返回结果，判断是否需要重试
            if (null != resultPredicate && resultPredicate.test(result)) {
                int currentNumOfAttempts = numOfAttempts.incrementAndGet();
                // 达到上限次数，不能重试
                if (currentNumOfAttempts >= maxAttempts) {
                    return false;
                } else {
                    // 未达到上限次数，等待一会重试
                    waitIntervalAfterFailure(currentNumOfAttempts, Either.right(result));
                    return true;
                }
            }
            // 不能重试
            return false;
        }

        @Override
        public void onError(Exception exception) throws Exception {
            if (exceptionPredicate.test(exception)) {
                lastException.set(exception);
                throwOrSleepAfterException();
            } else {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), exception));
                throw exception;
            }
        }

        @Override
        public void onRuntimeError(RuntimeException runtimeException) {
            // 判断是否有运行时异常
            if (exceptionPredicate.test(runtimeException)) {
                // 存在运行时异常
                lastRuntimeException.set(runtimeException);
                // 抛出运行时异常或者睡眠一下
                throwOrSleepAfterRuntimeException();
            } else {
                // 不可预料的异常，try没有异常发生失败
                failedWithoutRetryCounter.increment();
                // 发布事件
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), runtimeException));
                throw runtimeException;
            }
        }

        private void throwOrSleepAfterException() throws Exception {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            Exception throwable = lastException.get();
            if (currentNumOfAttempts >= maxAttempts) {
                failedAfterRetryCounter.increment();
                publishRetryEvent(
                    () -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            } else {
                waitIntervalAfterFailure(currentNumOfAttempts, Either.left(throwable));
            }
        }

        private void throwOrSleepAfterRuntimeException() {
            int currentNumOfAttempts = numOfAttempts.incrementAndGet();
            RuntimeException throwable = lastRuntimeException.get();
            // 如果达到上线，抛出异常
            if (currentNumOfAttempts >= maxAttempts) {
                failedAfterRetryCounter.increment();
                publishRetryEvent(
                    () -> new RetryOnErrorEvent(getName(), currentNumOfAttempts, throwable));
                throw throwable;
            } else {
                // 等待一会
                waitIntervalAfterFailure(currentNumOfAttempts, Either.left(throwable));
            }
        }

        private void waitIntervalAfterFailure(int currentNumOfAttempts, Either<Throwable, T> either) {
            // wait interval until the next attempt should start
            long interval = intervalBiFunction.apply(numOfAttempts.get(), either);
            publishRetryEvent(
                () -> new RetryOnRetryEvent(getName(), currentNumOfAttempts, either.swap().getOrNull(), interval));
            Try.run(() -> sleepFunction.accept(interval))
                .getOrElseThrow(ex -> lastRuntimeException.get());
        }

    }

    public final class AsyncContextImpl implements Retry.AsyncContext<T> {

        private final AtomicInteger numOfAttempts = new AtomicInteger(0);
        private final AtomicReference<Throwable> lastException = new AtomicReference<>();

        /**
         * @deprecated since 1.2.0
         */
        @Override
        @Deprecated
        public void onSuccess() {
            onComplete();
        }

        @Override
        public void onComplete() {
            int currentNumOfAttempts = numOfAttempts.get();
            if (currentNumOfAttempts > 0 && currentNumOfAttempts < maxAttempts) {
                succeededAfterRetryCounter.increment();
                publishRetryEvent(
                    () -> new RetryOnSuccessEvent(name, currentNumOfAttempts, lastException.get()));
            } else {
                if (currentNumOfAttempts >= maxAttempts) {
                    failedAfterRetryCounter.increment();
                    Throwable throwable = Option.of(lastException.get())
                        .filter(p -> !failAfterMaxAttempts)
                        .getOrElse(new MaxRetriesExceeded(
                            "max retries is reached out for the result predicate check"
                        ));

                    publishRetryEvent(() -> new RetryOnErrorEvent(name, currentNumOfAttempts, throwable));

                    if (failAfterMaxAttempts) {
                        throw MaxRetriesExceededException.createMaxRetriesExceededException(RetryImpl.this);
                    }
                } else {
                    succeededWithoutRetryCounter.increment();

                }
            }
        }

        @Override
        public long onError(Throwable throwable) {
            // Handle the case if the completable future throw CompletionException wrapping the original exception
            // where original exception is the the one to retry not the CompletionException.
            if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
                Throwable cause = throwable.getCause();
                return handleThrowable(cause);
            } else {
                return handleThrowable(throwable);
            }

        }

        private long handleThrowable(Throwable throwable) {
            if (!exceptionPredicate.test(throwable)) {
                failedWithoutRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnIgnoredErrorEvent(getName(), throwable));
                return -1;
            }
            return handleOnError(throwable);
        }

        private long handleOnError(Throwable throwable) {
            lastException.set(throwable);
            int attempt = numOfAttempts.incrementAndGet();
            if (attempt >= maxAttempts) {
                failedAfterRetryCounter.increment();
                publishRetryEvent(() -> new RetryOnErrorEvent(name, attempt, throwable));
                return -1;
            }

            long interval = intervalBiFunction.apply(attempt, Either.left(throwable));
            publishRetryEvent(() -> new RetryOnRetryEvent(getName(), attempt, throwable, interval));
            return interval;
        }

        @Override
        public long onResult(T result) {
            if (null != resultPredicate && resultPredicate.test(result)) {
                int attempt = numOfAttempts.incrementAndGet();
                if (attempt >= maxAttempts) {
                    return -1;
                }
                return intervalBiFunction.apply(attempt, Either.right(result));
            } else {
                return -1;
            }
        }
    }

    public final class RetryMetrics implements Metrics {

        private RetryMetrics() {
        }

        @Override
        public long getNumberOfSuccessfulCallsWithoutRetryAttempt() {
            return succeededWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithoutRetryAttempt() {
            return failedWithoutRetryCounter.longValue();
        }

        @Override
        public long getNumberOfSuccessfulCallsWithRetryAttempt() {
            return succeededAfterRetryCounter.longValue();
        }

        @Override
        public long getNumberOfFailedCallsWithRetryAttempt() {
            return failedAfterRetryCounter.longValue();
        }
    }

    private class RetryEventProcessor extends EventProcessor<RetryEvent> implements
        EventConsumer<RetryEvent>, EventPublisher {

        @Override
        public void consumeEvent(RetryEvent event) {
            super.processEvent(event);
        }

        @Override
        public EventPublisher onRetry(EventConsumer<RetryOnRetryEvent> onRetryEventConsumer) {
            registerConsumer(RetryOnRetryEvent.class.getName(), onRetryEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onSuccess(EventConsumer<RetryOnSuccessEvent> onSuccessEventConsumer) {
            registerConsumer(RetryOnSuccessEvent.class.getName(), onSuccessEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onError(EventConsumer<RetryOnErrorEvent> onErrorEventConsumer) {
            registerConsumer(RetryOnErrorEvent.class.getName(), onErrorEventConsumer);
            return this;
        }

        @Override
        public EventPublisher onIgnoredError(
            EventConsumer<RetryOnIgnoredErrorEvent> onIgnoredErrorEventConsumer) {
            registerConsumer(RetryOnIgnoredErrorEvent.class.getName(), onIgnoredErrorEventConsumer);
            return this;
        }
    }
}
