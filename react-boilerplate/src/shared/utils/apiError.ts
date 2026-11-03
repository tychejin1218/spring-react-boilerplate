import axios from 'axios';
import { ERROR_MESSAGES } from '@/shared/constants/messages';

/**
 * 오류 안내 분류
 * - guide: HTTP 4xx. 사용자가 조치할 수 있는 오류라 BE 메시지를 그대로 안내한다
 * - adminContact: HTTP 5xx. 사용자가 조치할 수 없으므로 관리자 문의를 안내한다
 * - network: 서버 응답 자체가 없다 (네트워크 단절, CORS, 타임아웃)
 * - unknown: 위로 분류할 수 없다 (런타임 오류, 알 수 없는 응답 형태)
 */
export type ErrorCategory = 'guide' | 'adminContact' | 'network' | 'unknown';

export interface ErrorPresentation {
  category: ErrorCategory;
  /** 사용자에게 보여 줄 문구 (항상 한국어 안내 문구. axios 영문 메시지는 노출하지 않는다) */
  message: string;
  /** BE ApiStatus 코드 (예: 'AUTH_REFRESH_TOKEN_INVALID'). 서버가 ErrorResponse 를 보냈을 때만 있다 */
  code?: string;
  /** HTTP 상태 코드. 알 수 있을 때만 있다 */
  httpStatus?: number;
  /** BE 가 보낸 message (앞뒤 공백 제거). 비어 있으면 없다 */
  serverMessage?: string;
}

interface Options {
  /** 4xx 인데 BE 메시지가 없거나, 분류할 수 없는 오류일 때 대신 보여 줄 화면별 문구 */
  fallbackMessage?: string;
}

interface ErrorInfo {
  httpStatus?: number;
  code?: string;
  serverMessage?: string;
}

const isObject = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null;

/** BE ErrorResponse 본문 모양인지 (statusCode 문자열이 있으면 서버가 만든 응답으로 본다) */
const readBody = (body: unknown): Pick<ErrorInfo, 'code' | 'serverMessage'> => {
  if (!isObject(body) || typeof body.statusCode !== 'string' || body.statusCode.length === 0) return {};
  const message = typeof body.message === 'string' ? body.message.trim() : '';
  return { code: body.statusCode, serverMessage: message || undefined };
};

/** 4xx 에서 BE 메시지가 없을 때 상태 코드별 기본 문구 */
const STATUS_FALLBACK_MESSAGES: Partial<Record<number, string>> = {
  401: ERROR_MESSAGES.UNAUTHORIZED,
  403: ERROR_MESSAGES.FORBIDDEN,
  429: ERROR_MESSAGES.RATE_LIMIT,
};

/** 응답 없이 끝난 axios 오류 중 타임아웃으로 보는 코드 */
const TIMEOUT_CODES = new Set(['ECONNABORTED', 'ETIMEDOUT']);

/**
 * 오류 → 사용자 안내 문구와 분류 (순수 함수)
 *
 * 입력으로 다음을 모두 받는다.
 * - instance.ts 가 reject 한 BE ErrorResponse (+ httpStatus)
 * - axios 오류 (응답 없음: 네트워크/타임아웃, 응답 있음: JSON 이 아닌 본문 등)
 * - 그 밖의 값 (Error, 문자열, undefined)
 *
 * 판정 순서
 * 1. 응답이 없는 axios 오류 → network (타임아웃이면 지연 안내)
 * 2. HTTP 5xx → adminContact
 * 3. HTTP 4xx 또는 BE 코드가 있음 → guide (BE 메시지 > 화면 fallback > 상태 코드별 기본 문구)
 * 4. 그 외 → unknown
 *
 * axios 오류의 message('Network Error', 'timeout of 10000ms exceeded' 등)와 Error.message 는
 * 영문 개발자용 문구라 어떤 분기에서도 사용자 문구로 쓰지 않는다.
 */
export const getErrorPresentation = (error: unknown, options: Options = {}): ErrorPresentation => {
  const { fallbackMessage } = options;
  let info: ErrorInfo;

  if (axios.isAxiosError(error)) {
    if (!error.response) {
      const isTimeout = TIMEOUT_CODES.has(error.code ?? '');
      return { category: 'network', message: isTimeout ? ERROR_MESSAGES.TIMEOUT : ERROR_MESSAGES.NETWORK_ERROR };
    }
    info = { httpStatus: error.response.status, ...readBody(error.response.data) };
  } else if (isObject(error)) {
    const httpStatus = typeof error.httpStatus === 'number' ? error.httpStatus : undefined;
    info = { httpStatus, ...readBody(error) };
  } else {
    info = {};
  }

  const { httpStatus, code, serverMessage } = info;
  const meta = { code, httpStatus, serverMessage };

  if (httpStatus !== undefined && httpStatus >= 500) {
    return { category: 'adminContact', message: ERROR_MESSAGES.SERVER_ERROR, ...meta };
  }

  const isClientError = httpStatus !== undefined && httpStatus >= 400;
  if (isClientError || code) {
    const statusFallback = httpStatus !== undefined ? STATUS_FALLBACK_MESSAGES[httpStatus] : undefined;
    return { category: 'guide', message: serverMessage ?? fallbackMessage ?? statusFallback ?? ERROR_MESSAGES.REQUEST_FAILED, ...meta };
  }

  return { category: 'unknown', message: fallbackMessage ?? ERROR_MESSAGES.UNKNOWN, ...meta };
};

/** 화면에서 오류 문구만 필요할 때 쓰는 단축 함수 (예: setError('root', { message: getErrorMessage(err, '로그인에 실패했습니다.') })) */
export const getErrorMessage = (error: unknown, fallbackMessage?: string): string => getErrorPresentation(error, { fallbackMessage }).message;
