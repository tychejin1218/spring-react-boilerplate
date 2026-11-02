package com.example.api.common.response;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페이지네이션 공통 응답 래퍼
 *
 * <p>Redis 캐시({@code @Cacheable})에 저장된 값을 Jackson 이 다시 읽을 수 있도록 기본 생성자를 둔다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean last;

  /**
   * 페이지 응답 생성
   *
   * @param content       현재 페이지 데이터 목록
   * @param totalElements 전체 데이터 수
   * @param page          현재 페이지 번호 (0부터 시작)
   * @param size          페이지 크기
   * @param <T>           데이터 타입
   * @return PageResponse
   */
  public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
    int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
    return PageResponse.<T>builder()
        .content(content)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .last(page >= totalPages - 1)
        .build();
  }
}
