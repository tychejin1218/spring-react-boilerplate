package com.example.api.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.api.sample.dto.SampleDto;
import com.example.api.sample.mapper.SampleMapper;
import com.example.api.sample.repository.SampleRepository;
import java.util.List;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Slf4j
@SpringJUnitConfig(SampleServiceCacheTest.CacheTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SampleService 캐시 테스트")
class SampleServiceCacheTest {

  @Autowired
  private SampleService sampleService;

  @Autowired
  private CacheManager cacheManager;

  @MockitoBean
  private SampleRepository sampleRepository;

  @MockitoBean
  private SampleMapper sampleMapper;

  @MockitoBean
  private com.example.api.domain.repository.MemberRepository memberRepository;

  @Configuration
  @EnableCaching
  @Import(SampleService.class)
  static class CacheTestConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("sample:memberList", "sample:member");
    }
  }

  @BeforeEach
  void setUp() {
    cacheManager.getCache("sample:memberList").clear();
    cacheManager.getCache("sample:member").clear();
  }

  @Test
  @Order(1)
  @DisplayName("getMemberList 첫 호출 - Repository 호출")
  void getMemberList_firstCall() {
    // given
    SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
        .name("admin")
        .email("admin@test.com")
        .build();
    SampleDto.MemberResponse mockResponse = SampleDto.MemberResponse.builder()
        .id(1L).name("admin").email("admin@test.com").build();
    given(sampleRepository.selectMemberList(request))
        .willReturn(List.of(mockResponse));

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMemberList(request);

    // then
    assertThat(result).hasSize(1);
    verify(sampleRepository, times(1)).selectMemberList(request);
  }

  @Test
  @Order(2)
  @DisplayName("getMemberList 반복 호출 - 캐시 적중으로 Repository 1회만 호출")
  void getMemberList_cachedCall() {
    // given
    SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
        .name("admin")
        .email("admin@test.com")
        .build();
    SampleDto.MemberResponse mockResponse = SampleDto.MemberResponse.builder()
        .id(1L).name("admin").email("admin@test.com").build();
    given(sampleRepository.selectMemberList(request))
        .willReturn(List.of(mockResponse));

    // when
    sampleService.getMemberList(request);
    sampleService.getMemberList(request);

    // then
    verify(sampleRepository, times(1)).selectMemberList(request);
  }

  @Test
  @Order(3)
  @DisplayName("getMember 반복 호출 - 캐시 적중으로 Repository 1회만 호출")
  void getMember_cachedCall() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(1L).build();
    SampleDto.MemberResponse mockResponse = SampleDto.MemberResponse.builder()
        .id(1L).name("admin").email("admin@test.com").build();
    given(sampleRepository.selectMember(request))
        .willReturn(mockResponse);

    // when
    sampleService.getMember(request);
    sampleService.getMember(request);

    // then
    verify(sampleRepository, times(1)).selectMember(request);
  }
}
