package com.example.api.common.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisKeysTest {

  @Test
  @DisplayName("네임스페이스, 경로, 식별자를 콜론 하나로 잇는다")
  void buildsKey() {
    assertThat(RedisKeys.REFRESH_TOKEN.key(3L)).isEqualTo("APP:AUTH:REFRESH_TOKEN:3");
    assertThat(RedisKeys.RATE_LIMIT.key("10.0.0.1", "/api/auth/login"))
        .isEqualTo("APP:RATE_LIMIT:10.0.0.1:/api/auth/login");
  }

  @Test
  @DisplayName("식별자 안의 콜론은 밑줄로 바꿔 구분자와 섞이지 않게 한다")
  void escapesDelimiterInIds() {
    assertThat(RedisKeys.RATE_LIMIT.key("0:0:0:0:0:0:0:1", "/api/auth/login"))
        .isEqualTo("APP:RATE_LIMIT:0_0_0_0_0_0_0_1:/api/auth/login");
  }

  @Test
  @DisplayName("식별자가 없으면 접두사만, null 식별자는 빈 문자열로 만든다")
  void handlesNoIdsAndNull() {
    assertThat(RedisKeys.RATE_LIMIT.key()).isEqualTo("APP:RATE_LIMIT");
    assertThat(RedisKeys.REFRESH_TOKEN.key((Object) null)).isEqualTo("APP:AUTH:REFRESH_TOKEN:");
  }

  @Test
  @DisplayName("캐시 접두사도 같은 네임스페이스를 쓴다")
  void cachePrefixSharesNamespace() {
    assertThat(RedisKeys.CACHE_PREFIX).isEqualTo("APP:CACHE:");
  }
}
