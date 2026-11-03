package com.example.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.api.common.type.ApiStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * BaseResponse 단위 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BaseResponse 단위 테스트")
class BaseResponseTest {

  @Test
  @Order(1)
  @DisplayName("ok(data) - 200 상태코드, 성공 메시지, 데이터 포함")
  void ok_withData() {
    // given
    String data = "test-data";

    // when
    BaseResponse<String> response = BaseResponse.ok(data);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(ApiStatus.OK.getCode()),
        () -> assertThat(response.getMessage()).isEqualTo(ApiStatus.OK.getMessage()),
        () -> assertThat(response.getData()).isEqualTo(data)
    );
  }

  @Test
  @Order(2)
  @DisplayName("ok() - 200 상태코드, 성공 메시지, 데이터 없음")
  void ok_withoutData() {
    // when
    BaseResponse<Void> response = BaseResponse.ok();

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(ApiStatus.OK.getCode()),
        () -> assertThat(response.getMessage()).isEqualTo(ApiStatus.OK.getMessage()),
        () -> assertThat(response.getData()).isNull()
    );
  }

  @Test
  @Order(3)
  @DisplayName("ok(data) - statusCode 값 'OK' 검증")
  void ok_statusCodeValue() {
    // when
    BaseResponse<Integer> response = BaseResponse.ok(42);

    // then
    assertThat(response.getStatusCode()).isEqualTo("OK");
  }

  @Test
  @Order(4)
  @DisplayName("message() - 메시지 변경 후 동일 객체 반환")
  void message_changesMessageAndReturnsSelf() {
    // given
    String newMessage = "변경된 메시지";
    BaseResponse<String> response = BaseResponse.ok("data");

    // when
    BaseResponse<String> result = response.message(newMessage);

    // then
    assertAll(
        () -> assertThat(result).isSameAs(response),
        () -> assertThat(result.getMessage()).isEqualTo(newMessage),
        () -> assertThat(result.getStatusCode()).isEqualTo(ApiStatus.OK.getCode()),
        () -> assertThat(result.getData()).isEqualTo("data")
    );
  }

  @Test
  @Order(5)
  @DisplayName("ok(data) - null 데이터 전달 시 data 필드 null")
  void ok_withNullData() {
    // when
    BaseResponse<String> response = BaseResponse.ok(null);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(ApiStatus.OK.getCode()),
        () -> assertThat(response.getData()).isNull()
    );
  }

  @Test
  @Order(6)
  @DisplayName("ok(data) - 객체 타입 데이터 정상 포함")
  void ok_withObjectData() {
    // given
    record TestData(Long id, String name) {

    }

    TestData data = new TestData(1L, "테스트");

    // when
    BaseResponse<TestData> response = BaseResponse.ok(data);

    // then
    assertAll(
        () -> assertThat(response.getData()).isNotNull(),
        () -> assertThat(response.getData().id()).isEqualTo(1L),
        () -> assertThat(response.getData().name()).isEqualTo("테스트")
    );
  }
}
