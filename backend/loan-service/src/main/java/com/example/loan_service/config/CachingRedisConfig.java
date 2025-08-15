package com.example.loan_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CachingRedisConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        
        // Cache cho dữ liệu ít thay đổi
        cacheConfigs.put("loanById", cacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("allLoans", cacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Cache cho repayment history (ít thay đổi)
        cacheConfigs.put("repaymentHistory", cacheConfig.entryTtl(Duration.ofMinutes(15)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
