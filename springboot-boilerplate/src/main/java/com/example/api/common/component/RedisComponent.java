package com.example.api.common.component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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

  /** 값이 ARGV[1] 과 같으면 ARGV[2] 로 바꾸고 PX(밀리초) TTL 을 다시 건다. 성공 1, 실패 0 */
  private static final RedisScript<Long> COMPARE_AND_SET_SCRIPT = RedisScript.of("""
      if redis.call('GET', KEYS[1]) == ARGV[1] then
        redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
        return 1
      end
      return 0
      """, Long.class);

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
   * 값이 예상과 같을 때만 새 값으로 교체 (compare-and-set)
   *
   * <p>조회와 교체를 Lua 스크립트 하나로 실행해 원자적으로 처리한다.
   * Refresh 토큰 교체처럼 동시에 두 요청이 들어와도 한쪽만 성공해야 하는 곳에 쓴다.
   *
   * @param key      Redis 키
   * @param expected 현재 저장돼 있어야 하는 값
   * @param newValue 교체할 값
   * @param duration 새 TTL
   * @param unit     TTL 단위
   * @return 교체했으면 true, 값이 달랐거나 키가 없거나 오류면 false
   */
  public boolean compareAndSet(String key, String expected, String newValue, long duration,
      TimeUnit unit) {
    try {
      Long result = stringRedisTemplate.execute(COMPARE_AND_SET_SCRIPT, List.of(key),
          expected, newValue, String.valueOf(unit.toMillis(duration)));
      return Long.valueOf(1L).equals(result);
    } catch (Exception e) {
      log.error("[compareAndSet] 실패 - key: {}, message: {}", key, e.getMessage(), e);
      return false;
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
