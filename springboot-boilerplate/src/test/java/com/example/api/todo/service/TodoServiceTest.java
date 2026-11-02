package com.example.api.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.api.common.exception.ApiException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 할 일 서비스 Mock 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("할 일 서비스 Mock 테스트")
class TodoServiceTest {

  private TodoService todoService;

  @Mock
  private TodoRepository todoRepository;

  @Mock
  private TodoQueryRepository todoQueryRepository;

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    todoService = new TodoService(
        todoRepository, todoQueryRepository, memberRepository,
        eventPublisher, new SimpleMeterRegistry());
    todoService.initMetrics();
  }

  @Test
  @Order(1)
  @DisplayName("할 일 목록 조회 - 성공")
  void getTodoList_success() {
    // given
    TodoDto.TodoListRequest request = new TodoDto.TodoListRequest();
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("홍길동")
        .password("encodedPassword")
        .build();
    Todo todo1 = Todo.builder()
        .id(2L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .member(member)
        .build();
    Todo todo2 = Todo.builder()
        .id(1L)
        .title("테스트 코드 작성하기")
        .completed(true)
        .member(member)
        .build();
    given(todoQueryRepository.selectTodoList(eq("test@example.com"), any())).willReturn(
        List.of(todo1, todo2));
    given(todoQueryRepository.selectTodoCount(eq("test@example.com"), any())).willReturn(2L);

    // when
    PageResponse<TodoDto.TodoResponse> result = todoService.getTodoList("test@example.com",
        request);

    // then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2L);
    assertThat(result.getPage()).isEqualTo(0);
    assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
    assertThat(result.getContent().get(0).getTitle()).isEqualTo("스프링 부트 공부하기");
    assertThat(result.getContent().get(0).isCompleted()).isFalse();
    assertThat(result.getContent().get(1).getId()).isEqualTo(1L);
    assertThat(result.getContent().get(1).isCompleted()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("할 일 목록 조회 - 빈 목록")
  void getTodoList_empty() {
    // given
    TodoDto.TodoListRequest request = new TodoDto.TodoListRequest();
    given(todoQueryRepository.selectTodoList(eq("test@example.com"), any())).willReturn(List.of());
    given(todoQueryRepository.selectTodoCount(eq("test@example.com"), any())).willReturn(0L);

    // when
    PageResponse<TodoDto.TodoResponse> result = todoService.getTodoList("test@example.com",
        request);

    // then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(0L);
  }

  @Test
  @Order(3)
  @DisplayName("할 일 추가 - 성공")
  void insertTodo_success() {
    // given
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("홍길동")
        .password("encodedPassword")
        .build();
    TodoDto.CreateRequest request = TodoDto.CreateRequest.builder()
        .title("스프링 부트 공부하기")
        .build();
    Todo savedTodo = Todo.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .member(member)
        .build();
    given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
    given(todoRepository.save(any(Todo.class))).willReturn(savedTodo);

    // when
    TodoDto.TodoResponse result = todoService.insertTodo("test@example.com", request);

    // then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getTitle()).isEqualTo("스프링 부트 공부하기");
    assertThat(result.isCompleted()).isFalse();
  }

  @Test
  @Order(4)
  @DisplayName("할 일 추가 - 존재하지 않는 회원")
  void insertTodo_memberNotFound() {
    // given
    TodoDto.CreateRequest request = TodoDto.CreateRequest.builder()
        .title("스프링 부트 공부하기")
        .build();
    given(memberRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.insertTodo("notfound@example.com", request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(5)
  @DisplayName("할 일 완료 처리 - 성공")
  void updateTodoComplete_success() {
    // given
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("홍길동")
        .password("encodedPassword")
        .build();
    Todo todo = Todo.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .member(member)
        .build();
    given(todoRepository.findByIdAndMemberEmail(1L, "test@example.com"))
        .willReturn(Optional.of(todo));

    // when
    TodoDto.TodoResponse result = todoService.updateTodoComplete("test@example.com", 1L);

    // then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.isCompleted()).isTrue();
  }

  @Test
  @Order(6)
  @DisplayName("할 일 완료 처리 - 존재하지 않는 할 일")
  void updateTodoComplete_notFound() {
    // given
    given(todoRepository.findByIdAndMemberEmail(999L, "test@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.updateTodoComplete("test@example.com", 999L))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(7)
  @DisplayName("할 일 완료 처리 - 다른 사용자의 할 일 접근")
  void updateTodoComplete_otherUserTodo() {
    // given
    given(todoRepository.findByIdAndMemberEmail(1L, "other@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.updateTodoComplete("other@example.com", 1L))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(8)
  @DisplayName("할 일 삭제 - 성공")
  void deleteTodo_success() {
    // given
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("홍길동")
        .password("encodedPassword")
        .build();
    Todo todo = Todo.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .member(member)
        .build();
    given(todoRepository.findByIdAndMemberEmail(1L, "test@example.com"))
        .willReturn(Optional.of(todo));

    // when
    todoService.deleteTodo("test@example.com", 1L);

    // then
    verify(todoRepository).findByIdAndMemberEmail(1L, "test@example.com");
  }

  @Test
  @Order(9)
  @DisplayName("할 일 삭제 - 존재하지 않는 할 일")
  void deleteTodo_notFound() {
    // given
    given(todoRepository.findByIdAndMemberEmail(999L, "test@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.deleteTodo("test@example.com", 999L))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(10)
  @DisplayName("할 일 삭제 - 다른 사용자의 할 일 접근")
  void deleteTodo_otherUserTodo() {
    // given
    given(todoRepository.findByIdAndMemberEmail(1L, "other@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.deleteTodo("other@example.com", 1L))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(11)
  @DisplayName("할 일 이미지 URL 업데이트 - 성공")
  void updateTodoImage_success() {
    // given
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("홍길동")
        .password("encodedPassword")
        .build();
    Todo todo = Todo.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .member(member)
        .build();
    TodoDto.UpdateImageRequest request = TodoDto.UpdateImageRequest.builder()
        .imageUrl("http://localhost:9000/boilerplate-bucket/uuid/image.png")
        .build();
    given(todoRepository.findByIdAndMemberEmail(1L, "test@example.com"))
        .willReturn(Optional.of(todo));

    // when
    TodoDto.TodoResponse result = todoService.updateTodoImage("test@example.com", 1L, request);

    // then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getImageUrl())
        .isEqualTo("http://localhost:9000/boilerplate-bucket/uuid/image.png");
  }

  @Test
  @Order(12)
  @DisplayName("할 일 이미지 URL 업데이트 - 존재하지 않는 할 일")
  void updateTodoImage_notFound() {
    // given
    TodoDto.UpdateImageRequest request = TodoDto.UpdateImageRequest.builder()
        .imageUrl("http://localhost:9000/boilerplate-bucket/uuid/image.png")
        .build();
    given(todoRepository.findByIdAndMemberEmail(999L, "test@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.updateTodoImage("test@example.com", 999L, request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(13)
  @DisplayName("할 일 이미지 URL 업데이트 - 다른 사용자의 할 일 접근")
  void updateTodoImage_otherUserTodo() {
    // given
    TodoDto.UpdateImageRequest request = TodoDto.UpdateImageRequest.builder()
        .imageUrl("http://localhost:9000/boilerplate-bucket/uuid/image.png")
        .build();
    given(todoRepository.findByIdAndMemberEmail(1L, "other@example.com"))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> todoService.updateTodoImage("other@example.com", 1L, request))
        .isInstanceOf(ApiException.class);
  }
}
