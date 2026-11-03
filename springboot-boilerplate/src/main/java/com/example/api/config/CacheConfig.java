package com.example.api.config;

import com.example.api.common.type.RedisKeys;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 캐시 설정
 *
 * <p>{@code @Cacheable}, {@code @CacheEvict} 등 Spring Cache 추상화를 Redis로 연동
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * RedisCacheManager 빈 등록
   *
   * @param connectionFactory Redis 커넥션 팩토리
   * @return RedisCacheManager 인스턴스
   */
  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        // Spring 기본 접두사는 "{캐시 이름}::" 라 다른 키와 구분자(::)와 네임스페이스가 다르다.
        // RedisKeys 규칙에 맞춰 "APP:CACHE:{캐시 이름}:" 로 통일한다
        .computePrefixWith(cacheName -> RedisKeys.CACHE_PREFIX + cacheName + RedisKeys.DELIMITER)
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .build();
  }
}
