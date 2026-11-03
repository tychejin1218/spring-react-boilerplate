package com.example.api.sample.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.sample.dto.SampleDto;
import com.example.api.sample.service.SampleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 샘플 컨트롤러 단위 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 컨트롤러 테스트")
class SampleControllerTest {

  @Autowired
  private WebApplicationContext wac;

  @MockitoBean
  private SampleService sampleService;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @Order(1)
  @DisplayName("GET /sample/member/{id} - 성공")
  void getMember_success() throws Exception {
    // given
    SampleDto.MemberResponse response = SampleDto.MemberResponse.builder()
        .id(1L)
        .name("admin")
        .email("admin@test.com")
        .build();
    given(sampleService.getMember(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/sample/member/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.name").value("admin"));
  }

  @Test
  @Order(2)
  @DisplayName("GET /sample/members/repository - 성공")
  void getMembersWithRepository_success() throws Exception {
    // given
    List<SampleDto.MemberResponse> responses = List.of(
        SampleDto.MemberResponse.builder()
            .id(1L)
            .name("admin")
            .email("admin@test.com")
            .build()
    );
    given(sampleService.getMembersWithRepository(any())).willReturn(responses);

    // when & then
    mockMvc.perform(get("/sample/members/repository")
            .param("name", "")
            .param("email", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data[0].name").value("admin"));
  }

  @Test
  @Order(3)
  @DisplayName("GET /sample/members/mapper - 성공")
  void getMembersWithMapper_success() throws Exception {
    // given
    List<SampleDto.MemberResponse> responses = List.of(
        SampleDto.MemberResponse.builder()
            .id(1L)
            .name("admin")
            .email("admin@test.com")
            .build()
    );
    given(sampleService.getMembersWithMapper(any())).willReturn(responses);

    // when & then
    mockMvc.perform(get("/sample/members/mapper")
            .param("name", "")
            .param("email", ""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("OK"))
        .andExpect(jsonPath("$.data[0].name").value("admin"));
  }
}
