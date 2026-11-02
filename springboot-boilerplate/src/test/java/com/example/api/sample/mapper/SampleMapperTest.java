package com.example.api.sample.mapper;

import static org.assertj.core.api.Assertions.assertThat;

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
 * 샘플 MyBatis 매퍼 단위 테스트
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 MyBatis 매퍼 테스트")
class SampleMapperTest {

  @Autowired
  private SampleMapper sampleMapper;

  @Autowired
  private MemberRepository memberRepository;

  @BeforeEach
  void setup() {
    memberRepository.save(Member.builder()
        .name("admin")
        .email("admin@test.com")
        .password("encodedPassword")
        .build());
    memberRepository.flush();
  }

  @Test
  @Order(1)
  @DisplayName("회원 목록 전체 조회")
  void selectMembers_all() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleMapper.selectMembers(request);

    // then
    assertThat(result).isNotEmpty();
  }

  @Test
  @Order(2)
  @DisplayName("회원 목록 이름 필터 조회")
  void selectMembers_withNameFilter() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .name("admin")
        .email("")
        .build();

    // when
    List<SampleDto.MemberResponse> result = sampleMapper.selectMembers(request);

    // then
    assertThat(result).isNotEmpty();
    result.forEach(member -> assertThat(member.getName()).contains("admin"));
  }
}
