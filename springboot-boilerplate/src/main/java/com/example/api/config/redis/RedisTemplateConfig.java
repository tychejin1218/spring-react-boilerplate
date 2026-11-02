package com.example.api.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis Template 빈 설정
 *
 * <p>문자열 전용 {@link StringRedisTemplate} 제공.
 * 객체 캐시는 {@code @Cacheable} 기반 {@link com.example.api.config.CacheConfig} 를 사용한다
 *
 * @see RedisStandaloneConfig
 * @see RedisClusterConfig
 */
@Configuration
public class RedisTemplateConfig {

  /**
   * 문자열 전용 RedisTemplate 설정
   *
   * @param connectionFactory Lettuce 커넥션 팩토리
   * @return {@link StringRedisTemplate} 객체
   */
  @Bean
  public StringRedisTemplate stringRedisTemplate(
      LettuceConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
