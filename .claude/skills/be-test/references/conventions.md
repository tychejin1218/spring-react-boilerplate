# 테스트 공통 컨벤션

## 메서드 네이밍

`{대상메서드}_{시나리오}` 형식으로 작성합니다.
테스트 이름만 봐도 무엇을 검증하는지 알 수 있어야 실패했을 때 원인 파악이 빠릅니다.

예시: `getMember_success`, `getMember_notFound`, `insertMember_duplicateEmail`

## @DisplayName

클래스·메서드 모두 한국어로 작성합니다.
CI 리포트에서 실패한 테스트를 빠르게 파악하기 위해 의미 있는 문장으로 씁니다.

| 테스트 유형 | 형식 | 예시 |
|-----------|------|------|
| Service / Repository | `"기능 - 시나리오"` | `"회원 조회 - 성공"` |
| Controller | `"HTTP메서드 /경로 - 시나리오"` | `"GET /member/{id} - 성공"` |

## 실행 순서

`@TestMethodOrder` + `@Order`로 순서를 명시합니다.
순서가 없으면 JUnit이 임의로 실행해 테스트 간 의존관계가 있을 때 간헐적으로 실패합니다.

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class XxxTest {

  @Test
  @Order(1)
  @DisplayName("회원 조회 - 성공")
  void getMember_success() { ... }

  @Test
  @Order(2)
  @DisplayName("회원 조회 - 존재하지 않는 회원")
  void getMember_notFound() { ... }
}
```

## Given / When / Then

모든 테스트 메서드에 주석으로 세 구간을 명확히 구분합니다.
구간이 명확하면 어디서 실패했는지 즉시 알 수 있습니다.

```java
@Test
@Order(1)
@DisplayName("회원 조회 - 성공")
void getMember_success() {
  // given
  Long memberId = 1L;
  Member member = Member.builder()...build();
  given(memberRepository.selectMember(any())).willReturn(Optional.of(member));

  // when
  MemberDto.Response result = memberService.getMember(MemberDto.Request.of(memberId));

  // then
  assertThat(result.getId()).isEqualTo(memberId);
  verify(memberRepository).selectMember(any());
}
```

## 정상 + 예외 케이스

각 메서드마다 정상과 예외 케이스를 모두 작성합니다.
정상만 있으면 방어 코드가 실제로 동작하는지 검증하지 못합니다.
