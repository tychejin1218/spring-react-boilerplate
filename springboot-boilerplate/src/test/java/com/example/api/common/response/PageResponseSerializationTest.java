package com.example.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.todo.dto.TodoDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * PageResponse 의 Redis 캐시 직렬화 왕복 테스트
 *
 * <p>CacheConfig 와 같은 직렬화기를 사용해, 캐시에 저장한 값을 다시 읽을 수 있는지 확인한다.
 */
class PageResponseSerializationTest {

  private final GenericJackson2JsonRedisSerializer serializer =
      new GenericJackson2JsonRedisSerializer();

  @Test
  @DisplayName("Redis 직렬화기로 저장한 PageResponse 를 다시 역직렬화할 수 있다")
  void roundTrip() {
    TodoDto.TodoResponse todo = TodoDto.TodoResponse.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(true)
        .build();
    PageResponse<TodoDto.TodoResponse> original = PageResponse.of(List.of(todo), 11, 0, 10);

    Object restored = serializer.deserialize(serializer.serialize(original));

    assertThat(restored).isInstanceOf(PageResponse.class);
    PageResponse<?> page = (PageResponse<?>) restored;
    assertThat(page.getTotalElements()).isEqualTo(11);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.isLast()).isFalse();
    assertThat(page.getContent()).singleElement()
        .isInstanceOfSatisfying(TodoDto.TodoResponse.class, item -> {
          assertThat(item.getTitle()).isEqualTo("스프링 부트 공부하기");
          assertThat(item.isCompleted()).isTrue();
        });
  }
}
