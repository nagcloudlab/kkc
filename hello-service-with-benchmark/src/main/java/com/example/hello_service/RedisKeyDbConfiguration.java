package com.example.hello_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisKeyDbConfiguration {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Configure and return the Redis connection factory
        // This is a placeholder; actual implementation will depend on your Redis setup
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisConnectionFactory keydbConnectionFactory() {
        // Configure and return the Redis connection factory
        // This is a placeholder; actual implementation will depend on your Redis setup
        return new LettuceConnectionFactory("localhost", 6380);
    }

    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // Additional configuration can be done here if needed
        // e.g., setting serializers for keys and values
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, Long> keydbTemplate(RedisConnectionFactory keydbConnectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(keydbConnectionFactory);
        // Additional configuration can be done here if needed
        // e.g., setting serializers for keys and values
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }

}
