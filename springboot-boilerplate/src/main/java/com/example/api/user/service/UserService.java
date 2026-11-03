package com.example.api.user.service;

import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import com.example.api.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final MemberRepository memberRepository;

  /**
   * 내 정보 조회
   *
   * @param email JWT subject (이메일)
   * @return 내 정보 응답
   * @throws ApiException 회원 미존재 시 (NOT_FOUND)
   */
  @Transactional(readOnly = true)
  public UserDto.MeResponse getMe(String email) {
    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(ApiStatus.MEMBER_NOT_FOUND));
    return UserDto.MeResponse.from(member);
  }
}
