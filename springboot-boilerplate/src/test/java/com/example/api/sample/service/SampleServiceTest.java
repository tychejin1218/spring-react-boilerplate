package com.example.api.sample.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.api.common.exception.ApiException;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.sample.dto.SampleDto;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플 서비스 통합 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 서비스 통합 테스트")
class SampleServiceTest {

  @Autowired
  private SampleService sampleService;

  @Autowired
  private MemberRepository memberRepository;

  private Long savedMemberId;

  @BeforeEach
  void setup() {
    Member member = memberRepository.save(Member.builder()
        .name("admin")
        .email("admin@test.com")
        .password("encodedPassword")
        .build());
    memberRepository.flush();
    savedMemberId = member.getId();
  }

  @Test
  @Order(1)
  @DisplayName("회원 단건 조회 - 성공")
  void getMember_success() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(savedMemberId)
        .build();

    // when
    SampleDto.MemberResponse response = sampleService.getMember(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(savedMemberId);
    assertThat(response.getName()).isEqualTo("admin");
    assertThat(response.getEmail()).isEqualTo("admin@test.com");
  }

  @Test
  @Order(2)
  @DisplayName("회원 단건 조회 - 존재하지 않는 회원")
  void getMember_notFound() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(999999L)
        .build();

    // when & then
    assertThatThrownBy(() -> sampleService.getMember(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(3)
  @DisplayName("QueryDSL 회원 목록 조회 - 이름 필터 성공")
  void getMemberList_withNameFilter_success() {
    // given
    SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
        .name("admin")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMemberList(request);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result).allMatch(r -> r.getName().contains("admin"));
  }

  @Test
  @Order(4)
  @DisplayName("QueryDSL 회원 목록 조회 - 조건 없음 전체 조회")
  void getMemberList_noFilter_returnsAll() {
    // given
    SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
        .name("")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMemberList(request);

    // then
    assertThat(result).isNotEmpty();
  }

  @Test
  @Order(5)
  @DisplayName("QueryDSL 회원 목록 조회 - 일치하지 않는 조건으로 빈 결과")
  void getMemberList_noMatch_returnsEmpty() {
    // given
    SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
        .name("존재하지않는이름xyz")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMemberList(request);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @Order(6)
  @DisplayName("JPA Repository 회원 목록 조회 - 이름 필터 성공")
  void getMembersWithRepository_withNameFilter_success() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("admin")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMembersWithRepository(request);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result).allMatch(r -> r.getName().contains("admin"));
  }

  @Test
  @Order(7)
  @DisplayName("JPA Repository 회원 목록 조회 - 일치하지 않는 조건으로 빈 결과")
  void getMembersWithRepository_noMatch_returnsEmpty() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("존재하지않는이름xyz")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMembersWithRepository(request);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @Order(8)
  @DisplayName("MyBatis Mapper 회원 목록 조회 - 이름 필터 성공")
  void getMembersWithMapper_withNameFilter_success() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("admin")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMembersWithMapper(request);

    // then
    assertThat(result).isNotEmpty();
    assertThat(result).allMatch(r -> r.getName().contains("admin"));
  }

  @Test
  @Order(9)
  @DisplayName("MyBatis Mapper 회원 목록 조회 - 일치하지 않는 조건으로 빈 결과")
  void getMembersWithMapper_noMatch_returnsEmpty() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("존재하지않는이름xyz")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleService.getMembersWithMapper(request);

    // then
    assertThat(result).isEmpty();
  }
}
