package com.example.api.todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.common.exception.ApiException;
import com.example.api.common.response.PageResponse;
import com.example.api.common.type.ApiStatus;
import com.example.api.todo.dto.TodoDto;
import com.example.api.todo.service.TodoService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * 할 일 컨트롤러 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("할 일 컨트롤러 테스트")
class TodoControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TodoService todoService;

  private MockMvc mockMvc;

  private UsernamePasswordAuthenticationToken auth;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    this.auth = new UsernamePasswordAuthenticationToken(
        "test@example.com", null, Collections.emptyList());
  }

  @Test
  @Order(1)
  @DisplayName("GET /todos - 성공")
  void getTodoList_success() throws Exception {
    // given
    List<TodoDto.TodoResponse> content = List.of(
        TodoDto.TodoResponse.builder().id(2L).title("스프링 부트 공부하기").completed(false).build(),
        TodoDto.TodoResponse.builder().id(1L).title("테스트 코드 작성하기").completed(true).build()
    );
    PageResponse<TodoDto.TodoResponse> pageResponse = PageResponse.of(content, 2L, 0, 10);
    given(todoService.getTodoList(eq("test@example.com"), any(TodoDto.TodoListRequest.class)))
        .willReturn(pageResponse);

    // when & then
    mockMvc.perform(get("/todos").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content[0].id").value(2))
        .andExpect(jsonPath("$.data.content[0].title").value("스프링 부트 공부하기"))
        .andExpect(jsonPath("$.data.content[0].completed").value(false))
        .andExpect(jsonPath("$.data.content[1].id").value(1))
        .andExpect(jsonPath("$.data.content[1].completed").value(true))
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.page").value(0));
  }

  @Test
  @Order(2)
  @DisplayName("GET /todos - 인증 없음")
  void getTodoList_unauthenticated() throws Exception {
    // when & then
    mockMvc.perform(get("/todos"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(3)
  @DisplayName("POST /todos - 성공")
  void insertTodo_success() throws Exception {
    // given
    TodoDto.CreateRequest request = TodoDto.CreateRequest.builder()
        .title("스프링 부트 공부하기")
        .build();
    TodoDto.TodoResponse response = TodoDto.TodoResponse.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(false)
        .build();
    given(todoService.insertTodo(eq("test@example.com"), any())).willReturn(response);

    // when & then
    mockMvc.perform(post("/todos")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.title").value("스프링 부트 공부하기"))
        .andExpect(jsonPath("$.data.completed").value(false));
  }

  @Test
  @Order(4)
  @DisplayName("POST /todos - 인증 없음")
  void insertTodo_unauthenticated() throws Exception {
    // given
    TodoDto.CreateRequest request = TodoDto.CreateRequest.builder()
        .title("스프링 부트 공부하기")
        .build();

    // when & then
    mockMvc.perform(post("/todos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(5)
  @DisplayName("POST /todos - 제목 누락 (유효성 검사 실패)")
  void insertTodo_titleBlank() throws Exception {
    // given
    TodoDto.CreateRequest request = TodoDto.CreateRequest.builder()
        .title("")
        .build();

    // when & then
    mockMvc.perform(post("/todos")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Order(6)
  @DisplayName("PATCH /todos/{id}/complete - 성공")
  void updateTodoComplete_success() throws Exception {
    // given
    TodoDto.TodoResponse response = TodoDto.TodoResponse.builder()
        .id(1L)
        .title("스프링 부트 공부하기")
        .completed(true)
        .build();
    given(todoService.updateTodoComplete("test@example.com", 1L)).willReturn(response);

    // when & then
    mockMvc.perform(patch("/todos/1/complete").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.completed").value(true));
  }

  @Test
  @Order(7)
  @DisplayName("PATCH /todos/{id}/complete - 존재하지 않는 할 일")
  void updateTodoComplete_notFound() throws Exception {
    // given
    willThrow(new ApiException(ApiStatus.RESOURCE_NOT_FOUND))
        .given(todoService).updateTodoComplete("test@example.com", 999L);

    // when & then
    mockMvc.perform(patch("/todos/999/complete").with(authentication(auth)))
        .andExpect(status().isNotFound());
  }

  @Test
  @Order(8)
  @DisplayName("PATCH /todos/{id}/complete - 인증 없음")
  void updateTodoComplete_unauthenticated() throws Exception {
    // when & then
    mockMvc.perform(patch("/todos/1/complete"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(9)
  @DisplayName("DELETE /todos/{id} - 성공")
  void deleteTodo_success() throws Exception {
    // given
    willDoNothing().given(todoService).deleteTodo("test@example.com", 1L);

    // when & then
    mockMvc.perform(delete("/todos/1").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"));
  }

  @Test
  @Order(10)
  @DisplayName("DELETE /todos/{id} - 존재하지 않는 할 일")
  void deleteTodo_notFound() throws Exception {
    // given
    willThrow(new ApiException(ApiStatus.RESOURCE_NOT_FOUND))
        .given(todoService).deleteTodo("test@example.com", 999L);

    // when & then
    mockMvc.perform(delete("/todos/999").with(authentication(auth)))
        .andExpect(status().isNotFound());
  }

  @Test
  @Order(11)
  @DisplayName("DELETE /todos/{id} - 인증 없음")
  void deleteTodo_unauthenticated() throws Exception {
    // when & then
    mockMvc.perform(delete("/todos/1"))
        .andExpect(status().isUnauthorized());
  }
}
