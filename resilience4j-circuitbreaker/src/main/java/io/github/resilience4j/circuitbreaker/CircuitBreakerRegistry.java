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
package io.github.resilience4j.circuitbreaker;


import io.github.resilience4j.circuitbreaker.internal.InMemoryCircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.RegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.vavr.collection.HashMap;
import io.vavr.collection.Seq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The {@link CircuitBreakerRegistry} is a factory to create CircuitBreaker instances which stores
 * all CircuitBreaker instances in a registry.
 */
public interface CircuitBreakerRegistry extends Registry<CircuitBreaker, CircuitBreakerConfig> {

    /**
     * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration.
     * 使用默认配置 -> 创建注册中心
     * @param circuitBreakerConfig a custom default CircuitBreaker configuration
     * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration.
     */
    static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig) {
        return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig);
    }

    /**
     * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration and a
     * CircuitBreaker registry event consumer.
     * 使用自定义配置作为默认配置 和 注册事件消费器 -> 创建注册中心
     * @param circuitBreakerConfig  a custom default CircuitBreaker configuration.
     * @param registryEventConsumer a CircuitBreaker registry event consumer.
     * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration and a
     * CircuitBreaker registry event consumer.
     */
    static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
        return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig, registryEventConsumer);
    }

    /**
     * Creates a CircuitBreakerRegistry with a custom default CircuitBreaker configuration and a
     * list of CircuitBreaker registry event consumers.
     * 使用自定义配置作为默认配置 和 注册事件消费器列表 -> 创建注册中心
     * @param circuitBreakerConfig   a custom default CircuitBreaker configuration.
     * @param registryEventConsumers a list of CircuitBreaker registry event consumers.
     * @return a CircuitBreakerRegistry with a custom CircuitBreaker configuration and list of
     * CircuitBreaker registry event consumers.
     */
    static CircuitBreakerRegistry of(CircuitBreakerConfig circuitBreakerConfig,
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
        return new InMemoryCircuitBreakerRegistry(circuitBreakerConfig, registryEventConsumers);
    }

    /**
     * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     * 使用自定义配置map作为共享配置 -> 创建注册中心
     * @param configs a Map of shared CircuitBreaker configurations
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     */
    static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs) {
        return of(configs, HashMap.empty());
    }

    /**
     * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     * <p>
     * Tags added to the registry will be added to every instance created by this registry.
     * 使用自定义配置map作为共享配置 和 标签 -> 创建注册中心 （标签会作为注册中心的属性）
     * @param configs a Map of shared CircuitBreaker configurations
     * @param tags    default tags to add to the registry
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations.
     */
    static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs,
        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryCircuitBreakerRegistry(configs, tags);
    }

    /**
     * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * CircuitBreaker registry event consumer.
     * 使用自定义配置map作为共享配置 和 事件消费器 -> 创建注册中心
     * @param configs               a Map of shared CircuitBreaker configurations.
     * @param registryEventConsumer a CircuitBreaker registry event consumer.
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * CircuitBreaker registry event consumer.
     */
    static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
        return new InMemoryCircuitBreakerRegistry(configs, registryEventConsumer);
    }

    /**
     * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * CircuitBreaker registry event consumer.
     * 使用自定义配置map作为共享配置、 事件消费器 和 标签 -> 创建注册中心
     * @param configs               a Map of shared CircuitBreaker configurations.
     * @param registryEventConsumer a CircuitBreaker registry event consumer.
     * @param tags                  default tags to add to the registry
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * CircuitBreaker registry event consumer.
     */
    static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs,
        RegistryEventConsumer<CircuitBreaker> registryEventConsumer,
        io.vavr.collection.Map<String, String> tags) {
        return new InMemoryCircuitBreakerRegistry(configs, registryEventConsumer, tags);
    }


    /**
     * Creates a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * list of CircuitBreaker registry event consumers.
     * 使用自定义配置map作为共享配置 和 事件消费器列表 -> 创建注册中心
     * @param configs                a Map of shared CircuitBreaker configurations.
     * @param registryEventConsumers a list of CircuitBreaker registry event consumers.
     * @return a CircuitBreakerRegistry with a Map of shared CircuitBreaker configurations and a
     * list of CircuitBreaker registry event consumers.
     */
    static CircuitBreakerRegistry of(Map<String, CircuitBreakerConfig> configs,
        List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers) {
        return new InMemoryCircuitBreakerRegistry(configs, registryEventConsumers);
    }

    /**
     * Creates a CircuitBreakerRegistry with a default CircuitBreaker configuration.
     * 使用默认配置 -> 创建注册中心
     * @return a CircuitBreakerRegistry with a default CircuitBreaker configuration.
     */
    static CircuitBreakerRegistry ofDefaults() {
        return new InMemoryCircuitBreakerRegistry();
    }

    /**
     * Returns all managed {@link CircuitBreaker} instances.
     * 返回所有注册中心管理的熔断器
     * @return all managed {@link CircuitBreaker} instances.
     */
    Seq<CircuitBreaker> getAllCircuitBreakers();

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with the default CircuitBreaker
     * configuration.
     * 创建或者返回注册中心管理的熔断器（创建的话，会使用默认的配置）
     * @param name the name of the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with the default CircuitBreaker
     * configuration.
     * 创建或者返回注册中心管理的熔断器。（如果注册中心已经带有tags了，方法的tags会和注册中心的合并，tags的key冲突时，以方法入参为主）
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name the name of the CircuitBreaker
     * @param tags tags added to the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker
     * configuration.
     * 创建或者返回注册中心管理的熔断器（熔断器配置按传入配置）
     * @param name   the name of the CircuitBreaker
     * @param config a custom CircuitBreaker configuration
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker
     * configuration.
     * 创建或者返回注册中心管理的熔断器（熔断器配置按传入配置，tags合并）
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name   the name of the CircuitBreaker
     * @param config a custom CircuitBreaker configuration
     * @param tags   tags added to the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, CircuitBreakerConfig config,
        io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * 创建或者返回注册中心管理的熔断器（按配置名查找对应的配置）
     * @param name       the name of the CircuitBreaker
     * @param configName the name of the shared configuration
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, String configName);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one.
     * The configuration must have been added upfront via {@link #addConfiguration(String, Object)}.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     * 创建或者返回注册中心管理的熔断器（按配置名查找对应的配置，tags合并）
     * @param name       the name of the CircuitBreaker
     * @param configName the name of the shared configuration
     * @param tags       tags added to the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name, String configName,
        io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker
     * configuration.
     * 创建或者返回注册中心管理的熔断器（配置以lambda形式延迟创建）
     * @param name                         the name of the CircuitBreaker
     * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier);

    /**
     * Returns a managed {@link CircuitBreaker} or creates a new one with a custom CircuitBreaker
     * configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     * 创建或者返回注册中心管理的熔断器（配置以lambda形式延迟创建，tags合并）
     * @param name                         the name of the CircuitBreaker
     * @param circuitBreakerConfigSupplier a supplier of a custom CircuitBreaker configuration
     * @param tags                         tags added to the CircuitBreaker
     * @return The {@link CircuitBreaker}
     */
    CircuitBreaker circuitBreaker(String name,
        Supplier<CircuitBreakerConfig> circuitBreakerConfigSupplier,
        io.vavr.collection.Map<String, String> tags);

    /**
     * Returns a builder to create a custom CircuitBreakerRegistry.
     * 创建者模式，自定义创建注册中心
     * @return a {@link CircuitBreakerRegistry.Builder}
     */
    static Builder custom() {
        return new Builder();
    }

    class Builder {

        private static final String DEFAULT_CONFIG = "default";
        private RegistryStore<CircuitBreaker> registryStore;
        private Map<String, CircuitBreakerConfig> circuitBreakerConfigsMap;
        private List<RegistryEventConsumer<CircuitBreaker>> registryEventConsumers;
        private io.vavr.collection.Map<String, String> tags;

        public Builder() {
            this.circuitBreakerConfigsMap = new java.util.HashMap<>();
            this.registryEventConsumers = new ArrayList<>();
        }

        // 设置注册中心的存储实体
        public Builder withRegistryStore(RegistryStore<CircuitBreaker> registryStore) {
            this.registryStore = registryStore;
            return this;
        }

        /**
         * Configures a CircuitBreakerRegistry with a custom default CircuitBreaker configuration.
         * 设置熔断器的默认配置
         * @param circuitBreakerConfig a custom default CircuitBreaker configuration
         * @return a {@link CircuitBreakerRegistry.Builder}
         */
        public Builder withCircuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
            circuitBreakerConfigsMap.put(DEFAULT_CONFIG, circuitBreakerConfig);
            return this;
        }

        /**
         * Configures a CircuitBreakerRegistry with a custom CircuitBreaker configuration.
         * 设置熔断器的配置（不允许配置default）
         * @param configName configName for a custom shared CircuitBreaker configuration
         * @param configuration a custom shared CircuitBreaker configuration
         * @return a {@link CircuitBreakerRegistry.Builder}
         * @throws IllegalArgumentException if {@code configName.equals("default")}
         */
        public Builder addCircuitBreakerConfig(String configName, CircuitBreakerConfig configuration) {
            if (configName.equals(DEFAULT_CONFIG)) {
                throw new IllegalArgumentException(
                    "You cannot add another configuration with name 'default' as it is preserved for default configuration");
            }
            circuitBreakerConfigsMap.put(configName, configuration);
            return this;
        }

        /**
         * Configures a CircuitBreakerRegistry with a CircuitBreaker registry event consumer.
         * 配置注册事件消费器
         * @param registryEventConsumer a CircuitBreaker registry event consumer.
         * @return a {@link CircuitBreakerRegistry.Builder}
         */
        public Builder addRegistryEventConsumer(RegistryEventConsumer<CircuitBreaker> registryEventConsumer) {
            this.registryEventConsumers.add(registryEventConsumer);
            return this;
        }

        /**
         * Configures a CircuitBreakerRegistry with Tags.
         * <p>
         * Tags added to the registry will be added to every instance created by this registry.
         * 配置tags标签
         * @param tags default tags to add to the registry.
         * @return a {@link CircuitBreakerRegistry.Builder}
         */
        public Builder withTags(io.vavr.collection.Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds a CircuitBreakerRegistry
         * 实际上创建的是InMemoryCircuitBreakerRegistry的实例对象
         * @return the CircuitBreakerRegistry
         */
        public CircuitBreakerRegistry build() {
            return new InMemoryCircuitBreakerRegistry(circuitBreakerConfigsMap, registryEventConsumers, tags,
                registryStore);
        }
    }

}
