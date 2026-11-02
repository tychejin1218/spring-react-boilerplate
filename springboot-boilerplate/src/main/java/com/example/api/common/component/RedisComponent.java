package com.example.api.common.component;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 공통 컴포넌트
 *
 * <p>문자열 값 저장/조회/삭제와 TTL 기반 카운터 증가 지원.
 * 모든 Redis 연산은 내부적으로 예외를 처리하여 장애 전파 방지.
 * 객체 캐시는 {@code @Cacheable} 기반 {@link com.example.api.config.CacheConfig} 를 사용한다
 *
 * @see com.example.api.config.redis.RedisTemplateConfig
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RedisComponent {

  private final StringRedisTemplate stringRedisTemplate;

  /**
   * 문자열 값 Redis 저장
   *
   * @param key      저장할 Redis 키
   * @param value    저장할 문자열 값
   * @param duration 유효 기간
   * @param unit     유효 기간 단위
   */
  public void setStringValue(String key, String value, long duration, TimeUnit unit) {
    try {
      stringRedisTemplate.opsForValue().set(key, value, duration, unit);
    } catch (Exception e) {
      log.error("[setStringValue] 실패 - key: {}, message: {}", key, e.getMessage(), e);
    }
  }

  /**
   * Redis 문자열 값 조회
   *
   * @param key 조회할 Redis 키
   * @return 저장된 문자열 값, 존재하지 않거나 오류 발생 시 null
   */
  public String getStringValue(String key) {
    try {
      String value = stringRedisTemplate.opsForValue().get(key);
      return StringUtils.isNotBlank(value) ? value : null;
    } catch (Exception e) {
      log.error("[getStringValue] 실패 - key: {}, message: {}", key, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Redis 키 삭제
   *
   * @param key 삭제할 Redis 키
   * @return 삭제 성공 여부
   */
  public boolean deleteKey(String key) {
    try {
      return stringRedisTemplate.delete(key);
    } catch (Exception e) {
      log.error("[deleteKey] 실패 - key: {}, message: {}", key, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Redis 정수 카운터 증가 (키가 없으면 생성 후 TTL 설정)
   *
   * @param key      Redis 키
   * @param duration TTL
   * @param unit     TTL 단위
   * @return 증가 후 카운트 값 (오류 시 null)
   */
  public Long increment(String key, long duration, TimeUnit unit) {
    try {
      Long count = stringRedisTemplate.opsForValue().increment(key);
      if (Long.valueOf(1L).equals(count)) {
        stringRedisTemplate.expire(key, duration, unit);
      }
      return count;
    } catch (Exception e) {
      log.error("[increment] 실패 - key: {}, message: {}", key, e.getMessage(), e);
      return null;
    }
  }
}
