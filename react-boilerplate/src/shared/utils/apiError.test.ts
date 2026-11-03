import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import { getErrorMessage, getErrorPresentation } from '@/shared/utils/apiError';

const errorResponse = (statusCode: string, message: string, httpStatus?: number) => ({
  statusCode,
  message,
  method: 'GET',
  path: '/test',
  timestamp: '2026-01-01T00:00:00',
  httpStatus,
});

const axiosError = (code: string, message: string, response?: Partial<AxiosResponse>) =>
  new AxiosError(
    message,
    code,
    undefined,
    undefined,
    response ? ({ headers: {}, config: { headers: new AxiosHeaders() }, statusText: '', ...response } as AxiosResponse) : undefined,
  );

describe('getErrorPresentation', () => {
  it('4xx 에 BE 메시지가 있으면 guide 로 BE 메시지를 보여 준다', () => {
    expect(getErrorPresentation(errorResponse('TODO_NOT_FOUND', '할 일을 찾을 수 없습니다.', 404))).toEqual({
      category: 'guide',
      message: '할 일을 찾을 수 없습니다.',
      code: 'TODO_NOT_FOUND',
      httpStatus: 404,
      serverMessage: '할 일을 찾을 수 없습니다.',
    });
  });

  it('4xx 에 BE 메시지가 비어 있으면 화면 fallback, 없으면 상태 코드별 기본 문구를 쓴다', () => {
    expect(getErrorPresentation(errorResponse('X', '  ', 400), { fallbackMessage: '저장에 실패했습니다.' }).message).toBe('저장에 실패했습니다.');
    expect(getErrorPresentation(errorResponse('X', '', 403)).message).toBe(ERROR_MESSAGES.FORBIDDEN);
    expect(getErrorPresentation(errorResponse('X', '', 400)).message).toBe(ERROR_MESSAGES.REQUEST_FAILED);
  });

  it('httpStatus 가 없어도 BE 코드가 있으면 guide 로 본다', () => {
    expect(getErrorPresentation(errorResponse('AUTH_LOGIN_FAILED', '이메일 또는 비밀번호가 올바르지 않습니다.'))).toMatchObject({
      category: 'guide',
      message: '이메일 또는 비밀번호가 올바르지 않습니다.',
    });
  });

  it('5xx 는 BE 메시지 대신 관리자 문의를 안내하고 코드는 남긴다', () => {
    expect(getErrorPresentation(errorResponse('SYSTEM_INTERNAL_ERROR', '내부 오류가 발생했습니다.', 500))).toMatchObject({
      category: 'adminContact',
      message: ERROR_MESSAGES.SERVER_ERROR,
      code: 'SYSTEM_INTERNAL_ERROR',
    });
  });

  it('응답이 없는 axios 오류는 network 로 분류하고 영문 메시지를 노출하지 않는다', () => {
    const network = getErrorPresentation(axiosError('ERR_NETWORK', 'Network Error'));
    expect(network).toEqual({ category: 'network', message: ERROR_MESSAGES.NETWORK_ERROR });

    const timeout = getErrorPresentation(axiosError('ECONNABORTED', 'timeout of 10000ms exceeded'));
    expect(timeout).toEqual({ category: 'network', message: ERROR_MESSAGES.TIMEOUT });
  });

  it('응답은 있지만 본문이 ErrorResponse 가 아닌 axios 오류는 HTTP 상태로 분류한다', () => {
    expect(
      getErrorPresentation(axiosError('ERR_BAD_RESPONSE', 'Request failed with status code 502', { status: 502, data: '<html>Bad Gateway</html>' })),
    ).toMatchObject({
      category: 'adminContact',
      message: ERROR_MESSAGES.SERVER_ERROR,
      httpStatus: 502,
    });
    expect(getErrorPresentation(axiosError('ERR_BAD_REQUEST', 'Request failed with status code 404', { status: 404, data: '' }))).toMatchObject({
      category: 'guide',
      message: ERROR_MESSAGES.REQUEST_FAILED,
    });
  });

  it('axios 오류 응답 본문이 ErrorResponse 면 BE 메시지를 쓴다', () => {
    const presentation = getErrorPresentation(
      axiosError('ERR_BAD_REQUEST', 'Request failed with status code 401', {
        status: 401,
        data: errorResponse('AUTH_REFRESH_TOKEN_INVALID', '세션이 만료되었습니다. 다시 로그인해주세요.'),
      }),
    );
    expect(presentation).toMatchObject({
      category: 'guide',
      code: 'AUTH_REFRESH_TOKEN_INVALID',
      httpStatus: 401,
      serverMessage: '세션이 만료되었습니다. 다시 로그인해주세요.',
    });
  });

  it.each([new Error('Cannot read properties of undefined'), 'boom', undefined, null, { foo: 1 }])('분류할 수 없는 값(%s)은 unknown 이다', (value) => {
    expect(getErrorPresentation(value)).toMatchObject({ category: 'unknown', message: ERROR_MESSAGES.UNKNOWN });
  });

  it('unknown 에도 화면 fallback 문구를 쓸 수 있다', () => {
    expect(getErrorMessage(new Error('x'), '데이터를 불러오지 못했습니다.')).toBe('데이터를 불러오지 못했습니다.');
  });
});
