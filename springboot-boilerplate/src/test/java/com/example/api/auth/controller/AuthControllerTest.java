package com.example.api.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.auth.dto.AuthDto;
import com.example.api.auth.service.AuthService;
import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증 컨트롤러 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("인증 컨트롤러 테스트")
class AuthControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @Order(1)
  @DisplayName("POST /auth/login - 성공")
  void login_success() throws Exception {
    // given
    AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
        .email("test@example.com")
        .password("password123")
        .build();
    AuthDto.TokenResponse tokenResponse = AuthDto.TokenResponse.builder()
        .accessToken("accessToken")
        .refreshToken("refreshToken")
        .build();
    given(authService.login(any())).willReturn(tokenResponse);

    // when & then
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
        .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
  }

  @Test
  @Order(2)
  @DisplayName("POST /auth/login - 인증 실패")
  void login_unauthorized() throws Exception {
    // given
    AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
        .email("test@example.com")
        .password("wrongPassword")
        .build();
    willThrow(new ApiException(ApiStatus.AUTH_LOGIN_FAILED))
        .given(authService).login(any());

    // when & then
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.statusCode").value("AUTH_LOGIN_FAILED"));
  }

  @Test
  @Order(3)
  @DisplayName("POST /auth/logout - 성공")
  void logout_success() throws Exception {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("refreshToken")
        .build();
    willDoNothing().given(authService).logout(any(), any());

    // when & then
    mockMvc.perform(post("/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"));
  }

  @Test
  @Order(4)
  @DisplayName("POST /auth/token/refresh - 성공")
  void refresh_success() throws Exception {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("refreshToken")
        .build();
    AuthDto.TokenResponse tokenResponse = AuthDto.TokenResponse.builder()
        .accessToken("newAccessToken")
        .refreshToken("newRefreshToken")
        .build();
    given(authService.refresh(any())).willReturn(tokenResponse);

    // when & then
    mockMvc.perform(post("/auth/token/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
        .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));
  }
}
