package com.example.api.common.type;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis 키 및 TTL 정의
 *
 * <p>키 형식: {@code {NAMESPACE}:{도메인}:{용도}:{식별자...}} (예: {@code APP:AUTH:REFRESH_TOKEN:3}).
 * 구분자는 {@value #DELIMITER} 하나만 쓰고, 키는 반드시 {@link #key(Object...)} 로 만든다.
 * 문자열을 직접 이어 붙이면 구분자가 빠지거나 겹치기 쉽다.
 *
 * <ul>
 *   <li>{@link #NAMESPACE} — 서비스 이름. 이 보일러플레이트로 새 서비스를 만들면 서비스 이름으로 바꾼다.
 *       여러 서비스가 Redis 하나를 같이 쓸 때 키가 섞이지 않게 하는 접두사다.
 *   <li>식별자 안의 {@value #DELIMITER} 는 {@value #ESCAPED_DELIMITER} 로 바꾼다.
 *       IPv6 주소(0:0:0:0:0:0:0:1)처럼 값에 콜론이 있으면 어디까지가 값인지 구분할 수 없기 때문이다.
 *   <li>식별자에는 이메일 같은 개인정보 대신 회원 번호를 쓴다. 키는 모니터링 화면과 로그에 그대로 보인다.
 *   <li>{@code @Cacheable} 캐시 키도 같은 접두사를 쓴다 ({@link #CACHE_PREFIX}, CacheConfig 참고).
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum RedisKeys {

  // TTL 0 은 코드에서 계산한다는 뜻이다
  REFRESH_TOKEN("AUTH:REFRESH_TOKEN", 0),  // {회원 번호}, TTL = jwt.session-idle-timeout-ms
  ACCESS_TOKEN_DENY("AUTH:ACCESS_TOKEN_DENY", 0),  // {jti}, TTL = Access 토큰 남은 유효 시간
  EMAIL_VERIFY_TOKEN("AUTH:EMAIL_VERIFY", 86_400),  // {인증 토큰}, 24시간
  PASSWORD_RESET_TOKEN("AUTH:PASSWORD_RESET", 1_800),  // {재설정 토큰}, 30분
  RATE_LIMIT("RATE_LIMIT", 60),  // {IP}:{URI}, 1분
  NOTIFICATION_CHANNEL("NOTIFICATION:CHANNEL", 0);  // {이메일}, Pub/Sub 채널 (TTL 없음)

  /** 서비스 이름 접두사. 새 서비스에서는 서비스 이름으로 바꾼다 */
  public static final String NAMESPACE = "APP";

  /** 키 조각 구분자 */
  public static final String DELIMITER = ":";

  /** 식별자 안의 구분자를 바꿀 문자 */
  public static final String ESCAPED_DELIMITER = "_";

  /** {@code @Cacheable} 캐시 키 접두사. 실제 키는 {@code APP:CACHE:{캐시 이름}:{캐시 키}} */
  public static final String CACHE_PREFIX = NAMESPACE + DELIMITER + "CACHE" + DELIMITER;

  private final String path;
  private final long ttl;

  /**
   * Redis 키 생성
   *
   * @param ids 식별자 (여러 개면 순서대로 구분자로 잇는다. null 은 빈 문자열)
   * @return {@code APP:{path}:{id1}:{id2}...}
   */
  public String key(Object... ids) {
    String prefix = NAMESPACE + DELIMITER + path;
    if (ids.length == 0) {
      return prefix;
    }
    return prefix + DELIMITER + Arrays.stream(ids)
        .map(id -> Objects.toString(id, "").replace(DELIMITER, ESCAPED_DELIMITER))
        .collect(Collectors.joining(DELIMITER));
  }
}
