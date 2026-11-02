package com.example.api.sample.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.sample.dto.SampleDto;
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
 * 샘플 QueryDSL 리포지토리 단위 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 QueryDSL 리포지토리 테스트")
class SampleRepositoryTest {

  @Autowired
  private SampleRepository sampleRepository;

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
  @DisplayName("회원 단건 조회 - 존재하는 회원")
  void selectMember_found() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(savedMemberId)
        .build();

    // when
    SampleDto.MemberResponse response = sampleRepository.selectMember(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(savedMemberId);
  }

  @Test
  @Order(2)
  @DisplayName("회원 단건 조회 - 존재하지 않는 회원 (null 반환)")
  void selectMember_notFound() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(999999L)
        .build();

    // when
    SampleDto.MemberResponse response = sampleRepository.selectMember(request);

    // then
    assertThat(response).isNull();
  }
}
