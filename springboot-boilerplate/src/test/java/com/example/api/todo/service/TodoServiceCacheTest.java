package com.example.api.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.api.common.response.PageResponse;
import com.example.api.domain.entity.Member;
import com.example.api.domain.entity.Todo;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.domain.repository.TodoRepository;
import com.example.api.todo.dto.TodoDto;
import com.example.api.todo.repository.TodoQueryRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig(TodoServiceCacheTest.CacheTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TodoService 캐시 테스트")
class TodoServiceCacheTest {

  @Autowired
  private TodoService todoService;

  @Autowired
  private CacheManager cacheManager;

  @MockitoBean
  private TodoRepository todoRepository;

  @MockitoBean
  private TodoQueryRepository todoQueryRepository;

  @MockitoBean
  private MemberRepository memberRepository;

  @MockitoBean
  private ApplicationEventPublisher eventPublisher;

  private static final String EMAIL = "test@example.com";

  private TodoDto.TodoListRequest listRequest;
  private Member member;
  private Todo todo;

  @Configuration
  @EnableCaching
  @Import(TodoService.class)
  static class CacheTestConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("todo:list");
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  @BeforeEach
  void setUp() {
    cacheManager.getCache("todo:list").clear();

    listRequest = new TodoDto.TodoListRequest();
    listRequest.setPage(0);
    listRequest.setSize(10);
    listRequest.setStatus("all");
    listRequest.setSort("id");

    member = Member.builder()
        .id(1L)
        .name("tester")
        .email(EMAIL)
        .password("encoded")
        .build();

    todo = Todo.builder()
        .id(1L)
        .title("Test Todo")
        .completed(false)
        .member(member)
        .build();
  }

  @Test
  @Order(1)
  @DisplayName("getTodoList 첫 호출 - Repository 호출")
  void getTodoList_firstCall() {
    // given
    given(todoQueryRepository.selectTodoList(eq(EMAIL), any()))
        .willReturn(List.of(todo));
    given(todoQueryRepository.selectTodoCount(eq(EMAIL), any()))
        .willReturn(1L);

    // when
    PageResponse<TodoDto.TodoResponse> result =
        todoService.getTodoList(EMAIL, listRequest);

    // then
    assertThat(result.getContent()).hasSize(1);
    verify(todoQueryRepository, times(1)).selectTodoList(eq(EMAIL), any());
  }

  @Test
  @Order(2)
  @DisplayName("getTodoList 반복 호출 - 캐시 적중으로 Repository 1회만 호출")
  void getTodoList_cachedCall() {
    // given
    given(todoQueryRepository.selectTodoList(eq(EMAIL), any()))
        .willReturn(List.of(todo));
    given(todoQueryRepository.selectTodoCount(eq(EMAIL), any()))
        .willReturn(1L);

    // when
    todoService.getTodoList(EMAIL, listRequest);
    todoService.getTodoList(EMAIL, listRequest);

    // then
    verify(todoQueryRepository, times(1)).selectTodoList(eq(EMAIL), any());
  }

  @Test
  @Order(3)
  @DisplayName("insertTodo 호출 - todo:list 캐시 무효화")
  void insertTodo_evictsCache() {
    // given - 캐시 적재
    given(todoQueryRepository.selectTodoList(eq(EMAIL), any()))
        .willReturn(List.of(todo));
    given(todoQueryRepository.selectTodoCount(eq(EMAIL), any()))
        .willReturn(1L);
    todoService.getTodoList(EMAIL, listRequest);

    given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
    given(todoRepository.save(any(Todo.class))).willReturn(todo);

    TodoDto.CreateRequest createRequest = TodoDto.CreateRequest.builder()
        .title("New Todo")
        .build();

    // when
    todoService.insertTodo(EMAIL, createRequest);
    todoService.getTodoList(EMAIL, listRequest);

    // then - 캐시 무효화 후 Repository 재호출 (총 2회)
    verify(todoQueryRepository, times(2)).selectTodoList(eq(EMAIL), any());
  }

  @Test
  @Order(4)
  @DisplayName("updateTodoComplete 호출 - 캐시 무효화")
  void updateTodoComplete_evictsCache() {
    // given - 캐시 적재
    given(todoQueryRepository.selectTodoList(eq(EMAIL), any()))
        .willReturn(List.of(todo));
    given(todoQueryRepository.selectTodoCount(eq(EMAIL), any()))
        .willReturn(1L);
    todoService.getTodoList(EMAIL, listRequest);

    given(todoRepository.findByIdAndMemberEmail(1L, EMAIL))
        .willReturn(Optional.of(todo));

    // when
    todoService.updateTodoComplete(EMAIL, 1L);
    todoService.getTodoList(EMAIL, listRequest);

    // then - 캐시 무효화 후 Repository 재호출 (총 2회)
    verify(todoQueryRepository, times(2)).selectTodoList(eq(EMAIL), any());
  }

  @Test
  @Order(5)
  @DisplayName("deleteTodo 호출 - 캐시 무효화")
  void deleteTodo_evictsCache() {
    // given - 캐시 적재
    given(todoQueryRepository.selectTodoList(eq(EMAIL), any()))
        .willReturn(List.of(todo));
    given(todoQueryRepository.selectTodoCount(eq(EMAIL), any()))
        .willReturn(1L);
    todoService.getTodoList(EMAIL, listRequest);

    given(todoRepository.findByIdAndMemberEmail(1L, EMAIL))
        .willReturn(Optional.of(todo));

    // when
    todoService.deleteTodo(EMAIL, 1L);
    todoService.getTodoList(EMAIL, listRequest);

    // then - 캐시 무효화 후 Repository 재호출 (총 2회)
    verify(todoQueryRepository, times(2)).selectTodoList(eq(EMAIL), any());
  }
}
