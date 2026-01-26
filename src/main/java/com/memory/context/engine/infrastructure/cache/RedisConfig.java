package com.memory.context.engine.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for caching and rate limiting.
 * Configures JSON serialization for type-safe cache entries.
 */
@Configuration
@EnableCaching
public class RedisConfig {

        /**
         * Cache TTL configurations by cache name.
         */
        public static final String MEMORY_CACHE = "memories";
        public static final String MEMORY_LIST_CACHE = "memory-lists";
        public static final Duration MEMORY_TTL = Duration.ofMinutes(30);
        public static final Duration LIST_TTL = Duration.ofMinutes(5);

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                // Default typing enabled to support polymorphic deserialization if needed
                mapper.activateDefaultTyping(
                                mapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
                return mapper;
        }

        @Bean
        @SuppressWarnings("deprecation")
        public RedisTemplate<String, Object> redisTemplate(
                        RedisConnectionFactory connectionFactory,
                        ObjectMapper objectMapper) {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(connectionFactory);
                template.setKeySerializer(new StringRedisSerializer());
                template.setHashKeySerializer(new StringRedisSerializer());

                // Use Jackson2JsonRedisSerializer for robust handling of arbitrary objects
                org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<Object> serializer = new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
                                objectMapper, Object.class);

                template.setValueSerializer(serializer);
                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();
                return template;
        }

        @Bean
        @SuppressWarnings("deprecation")
        public CacheManager cacheManager(
                        RedisConnectionFactory connectionFactory,
                        ObjectMapper objectMapper) {
                // Use Generic serializer for robust handling of arbitrary objects (despite
                // deprecation)
                org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<Object> serializer = new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(
                                objectMapper, Object.class);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer))
                                .disableCachingNullValues();

                RedisCacheConfiguration memoryConfig = defaultConfig.entryTtl(MEMORY_TTL);
                RedisCacheConfiguration listConfig = defaultConfig.entryTtl(LIST_TTL);

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration(MEMORY_CACHE, memoryConfig)
                                .withCacheConfiguration(MEMORY_LIST_CACHE, listConfig)
                                .build();
        }
}
