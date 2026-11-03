package com.example.api.sample.service;

import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.sample.dto.SampleDto;
import com.example.api.sample.mapper.SampleMapper;
import com.example.api.sample.repository.SampleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleService {

  private final MemberRepository memberRepository;
  private final SampleRepository sampleRepository;
  private final SampleMapper sampleMapper;

  /**
   * QueryDSL을 이용한 회원 목록 조회
   *
   * @param request 조회 조건
   * @return 회원 목록
   */
  @Cacheable(value = "sample:memberList", key = "#request.name + ':' + #request.email")
  @Transactional(readOnly = true)
  public List<SampleDto.MemberResponse> getMemberList(SampleDto.MemberListRequest request) {
    return sampleRepository.selectMemberList(request);
  }

  /**
   * 회원 단건 조회
   *
   * @param request 조회 조건
   * @return 회원 정보
   * @throws ApiException 회원 미존재 시 (NOT_FOUND)
   */
  @Cacheable(value = "sample:member", key = "#request.id")
  @Transactional(readOnly = true)
  public SampleDto.MemberResponse getMember(SampleDto.MemberRequest request) {
    SampleDto.MemberResponse response = sampleRepository.selectMember(request);
    if (response == null) {
      throw new ApiException(ApiStatus.RESOURCE_NOT_FOUND);
    }
    return response;
  }

  /**
   * JPA Repository를 이용한 회원 목록 조회
   *
   * @param request 조회 조건
   * @return 회원 목록
   */
  @Transactional(readOnly = true)
  public List<SampleDto.MemberResponse> getMembersWithRepository(
      SampleDto.MemberRequest request) {
    List<Member> members = memberRepository.findAllByNameContainingAndEmailContaining(
        request.getName(), request.getEmail(), Sort.by(Sort.Direction.DESC, "id"));

    return members.stream()
        .map(SampleDto.MemberResponse::from)
        .toList();
  }

  /**
   * MyBatis Mapper를 이용한 회원 목록 조회
   *
   * @param request 조회 조건
   * @return 회원 목록
   */
  @Transactional(readOnly = true)
  public List<SampleDto.MemberResponse> getMembersWithMapper(
      SampleDto.MemberRequest request) {
    return sampleMapper.selectMembers(request);
  }
}
