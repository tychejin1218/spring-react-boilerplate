package com.example.api.todo.service;

import com.example.api.common.exception.ApiException;
import com.example.api.common.response.PageResponse;
import com.example.api.common.type.ApiStatus;
import com.example.api.domain.entity.Member;
import com.example.api.domain.entity.Todo;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.domain.repository.TodoRepository;
import com.example.api.notification.event.TodoCompleteEvent;
import com.example.api.todo.dto.TodoDto;
import com.example.api.todo.repository.TodoQueryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 할 일 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;
  private final TodoQueryRepository todoQueryRepository;
  private final MemberRepository memberRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;
  private Counter todoCreateCounter;

  @PostConstruct
  void initMetrics() {
    todoCreateCounter = Counter.builder("todo.create.count")
        .description("Todo 생성 횟수")
        .register(meterRegistry);
  }

  /**
   * 내 할 일 목록 조회 (페이지네이션 + 상태 필터)
   *
   * @param email   인증 회원 이메일
   * @param request 조회 조건 (page, size, status)
   * @return 페이지 응답
   */
  @Cacheable(value = "todo:list",
      key = "#email + ':' + #request.page + ':' + #request.size"
          + " + ':' + #request.status + ':' + #request.sort")
  @Transactional(readOnly = true)
  public PageResponse<TodoDto.TodoResponse> getTodoList(
      String email, TodoDto.TodoListRequest request) {
    List<Todo> todos = todoQueryRepository.selectTodoList(email, request);
    long total = todoQueryRepository.selectTodoCount(email, request);
    List<TodoDto.TodoResponse> content = todos.stream()
        .map(TodoDto.TodoResponse::from)
        .toList();
    return PageResponse.of(content, total, request.getPage(), request.getSize());
  }

  /**
   * 할 일 추가
   *
   * @param email   인증 회원 이메일
   * @param request 할 일 생성 요청
   * @return 생성된 할 일
   */
  @CacheEvict(value = "todo:list", allEntries = true)
  @Transactional
  public TodoDto.TodoResponse insertTodo(String email, TodoDto.CreateRequest request) {
    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(ApiStatus.AUTH_UNAUTHORIZED));
    Todo todo = todoRepository.save(Todo.of(request.getTitle(), member));
    todoCreateCounter.increment();
    return TodoDto.TodoResponse.from(todo);
  }

  /**
   * 할 일 완료 처리
   *
   * @param email 인증 회원 이메일
   * @param id    할 일 ID
   * @return 업데이트된 할 일
   */
  @CacheEvict(value = "todo:list", allEntries = true)
  @Transactional
  public TodoDto.TodoResponse updateTodoComplete(String email, Long id) {
    Todo todo = findTodo(id, email);
    todo.complete();
    // [예제: notification] 완료 알림. notification 을 지울 때 이 줄과 import, eventPublisher 필드를 지운다
    eventPublisher.publishEvent(new TodoCompleteEvent(email, todo.getId(), todo.getTitle()));
    return TodoDto.TodoResponse.from(todo);
  }

  /**
   * 할 일 이미지 URL 업데이트
   *
   * @param email   인증 회원 이메일
   * @param id      할 일 ID
   * @param request 이미지 URL 업데이트 요청
   * @return 업데이트된 할 일
   */
  @CacheEvict(value = "todo:list", allEntries = true)
  @Transactional
  public TodoDto.TodoResponse updateTodoImage(
      String email, Long id, TodoDto.UpdateImageRequest request) {
    Todo todo = findTodo(id, email);
    todo.updateImage(request.getImageUrl());
    return TodoDto.TodoResponse.from(todo);
  }

  /**
   * 할 일 삭제
   *
   * @param email 인증 회원 이메일
   * @param id    할 일 ID
   */
  @CacheEvict(value = "todo:list", allEntries = true)
  @Transactional
  public void deleteTodo(String email, Long id) {
    Todo todo = findTodo(id, email);
    todo.softDelete();
  }

  /**
   * 이메일과 ID로 할 일 조회 (소유권 검증 포함)
   *
   * @param id    할 일 ID
   * @param email 인증 회원 이메일
   * @return 할 일 엔티티
   */
  private Todo findTodo(Long id, String email) {
    return todoRepository.findByIdAndMemberEmail(id, email)
        .orElseThrow(() -> new ApiException(ApiStatus.TODO_NOT_FOUND));
  }
}
