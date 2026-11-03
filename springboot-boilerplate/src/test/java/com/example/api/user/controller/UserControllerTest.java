package com.example.api.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.user.dto.UserDto;
import com.example.api.user.service.UserService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 회원 컨트롤러 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("회원 컨트롤러 테스트")
class UserControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @MockitoBean
  private UserService userService;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
  }

  @Test
  @Order(1)
  @DisplayName("GET /user/me - 성공")
  void getMe_success() throws Exception {
    // given
    UserDto.MeResponse response = UserDto.MeResponse.builder()
        .id(1L)
        .name("홍길동")
        .email("test@example.com")
        .build();
    given(userService.getMe(any())).willReturn(response);

    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        "test@example.com", null, Collections.emptyList());

    // when & then
    mockMvc.perform(get("/users/me").with(authentication(auth)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.name").value("홍길동"))
        .andExpect(jsonPath("$.data.email").value("test@example.com"));
  }

  @Test
  @Order(2)
  @DisplayName("GET /user/me - 인증 없음")
  void getMe_unauthenticated() throws Exception {
    // when & then
    mockMvc.perform(get("/users/me"))
        .andExpect(status().isUnauthorized());
  }
}
