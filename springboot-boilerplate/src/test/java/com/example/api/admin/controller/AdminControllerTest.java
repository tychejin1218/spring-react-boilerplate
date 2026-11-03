package com.example.api.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.admin.service.AdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리자 컨트롤러 테스트
 *
 * <p>실제 보안 설정 위에서 {@code @PreAuthorize} 거부가 500 이 아니라 403 으로 나가는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("관리자 컨트롤러 테스트")
class AdminControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @MockitoBean
  private AdminService adminService;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
  }

  private static UsernamePasswordAuthenticationToken auth(String role) {
    return new UsernamePasswordAuthenticationToken(
        "member@example.com", null, List.of(new SimpleGrantedAuthority(role)));
  }

  @Test
  @DisplayName("GET /admin/members - 일반 회원은 403 PERMISSION_FORBIDDEN")
  void getMemberList_forbiddenForUser() throws Exception {
    mockMvc.perform(get("/admin/members").with(authentication(auth("ROLE_USER"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.statusCode").value("PERMISSION_FORBIDDEN"));
  }

  @Test
  @DisplayName("GET /admin/members - 관리자는 200")
  void getMemberList_okForAdmin() throws Exception {
    given(adminService.getMemberList()).willReturn(List.of());

    mockMvc.perform(get("/admin/members").with(authentication(auth("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"));
  }
}
