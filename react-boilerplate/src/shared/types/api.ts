/** API 성공 응답 */
export interface ApiResponse<T> {
  statusCode: string;
  message: string;
  data: T;
}

/** API 실패 응답 */
export interface ErrorResponse {
  statusCode: string;
  message: string;
  method: string;
  path: string;
  timestamp: string;
  /**
   * HTTP 상태 코드. BE 응답 본문에는 없고 instance.ts 응답 인터셉터가 붙인다.
   * 4xx/5xx 구분(getErrorPresentation)에 쓴다
   */
  httpStatus?: number;
}
