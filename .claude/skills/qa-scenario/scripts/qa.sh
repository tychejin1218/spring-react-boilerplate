#!/usr/bin/env bash
# qa-scenario 스킬 도우미. 실제 로컬 인프라(BE 9091, FE 5173, MySQL, Redis, MinIO, MailHog)를 대상으로 한다.
# 사용법: qa.sh <명령> [인자...]
#   preflight                          실행 전 점검 (컨테이너, BE, FE, MSW 꺼짐 여부)
#   create-user <email> <pw> [ADMIN]   API 로 가입 후 MailHog 메일 토큰으로 인증까지 끝낸 계정 생성
#   mail-link <email> <verify|reset>   해당 주소로 온 가장 최근 메일의 링크 출력
#   mail-count <email>                 해당 주소로 온 메일 수
#   login <email> <pw>                 로그인 응답 JSON 출력 (accessToken, refreshToken)
#   api <method> <path> [token] [json] BE API 호출. 상태 코드와 본문 출력
#   sql "<query>"                      MySQL 조회 (boilerplate_db)
#   redis <redis-cli 인자...>          Redis 명령 실행
#   make-image <path>                  업로드 테스트용 PNG 생성
#   cleanup                            qa- 접두사 테스트 계정과 관련 데이터 삭제
set -euo pipefail

BE="${QA_BE_URL:-http://localhost:9091/api}"
FE="${QA_FE_URL:-http://localhost:5173}"
MAILHOG="${QA_MAILHOG_URL:-http://localhost:8025}"
MYSQL_CONTAINER="${QA_MYSQL_CONTAINER:-springboot-boilerplate-mysql}"
REDIS_CONTAINER="${QA_REDIS_CONTAINER:-springboot-boilerplate-redis}"
# 테스트 계정은 반드시 이 접두사로 만든다. cleanup 은 이 접두사만 지운다.
QA_PREFIX="qa-"

sql() {
  docker exec "$MYSQL_CONTAINER" mysql -uboilerplate -pboilerplate --default-character-set=utf8mb4 \
    -N -B boilerplate_db -e "$1" 2>/dev/null
}

redis() { docker exec "$REDIS_CONTAINER" redis-cli "$@"; }

api() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -o /tmp/qa-api-body.$$ -w '%{http_code}' -X "$method" "$BE$path" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body" ]] && args+=(-d "$body")
  local code; code=$(curl "${args[@]}")
  echo "$code $(cat /tmp/qa-api-body.$$)"; rm -f /tmp/qa-api-body.$$
}

# MailHog 에서 해당 수신자의 가장 최근 메일 본문(quoted-printable 해제)을 가져와 링크를 뽑는다
mail_link() {
  local email="$1" kind="$2" pattern
  case "$kind" in
    verify) pattern='verify-email\?token=' ;;
    reset) pattern='token=' ;;
    *) echo "kind 는 verify 또는 reset" >&2; return 2 ;;
  esac
  curl -s "$MAILHOG/api/v2/search?kind=to&query=$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$email")&limit=1" |
    python3 -c '
import sys, json, quopri, re
data = json.load(sys.stdin)
if not data["items"]:
    sys.exit("메일 없음")
body = data["items"][0]["Content"]["Body"]
body = quopri.decodestring(body.encode()).decode("utf-8", "replace")
links = re.findall(r"href=\"([^\"]+)\"", body)
pat = re.compile(sys.argv[1])
hit = [l for l in links if pat.search(l)]
print(hit[0] if hit else sys.exit("링크 없음: " + str(links)))
' "$pattern"
}

mail_count() {
  curl -s "$MAILHOG/api/v2/search?kind=to&query=$1" | python3 -c 'import sys,json;print(json.load(sys.stdin)["total"])'
}

preflight() {
  local fail=0
  ok() { printf '  [OK]   %s\n' "$1"; }
  ng() { printf '  [FAIL] %s\n' "$1"; fail=1; }
  echo "QA 사전 점검"
  for c in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" springboot-boilerplate-minio springboot-boilerplate-mailhog; do
    if [[ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)" == "true" ]]; then ok "컨테이너 $c"; else ng "컨테이너 $c 꺼짐 (cd springboot-boilerplate && docker compose up -d)"; fi
  done
  local code; code=$(curl -s -o /dev/null -w '%{http_code}' "$BE/health" || true)
  [[ "$code" == "200" ]] && ok "BE $BE/health" || ng "BE 응답 없음 ($code). IntelliJ 등으로 local 프로파일 기동 필요"
  # 응답 코드 형식으로 BE 버전 확인 (enum 이름 형식이어야 함)
  local sc; sc=$(curl -s -X POST "$BE/auth/login" -H 'Content-Type: application/json' -d '{"email":"qa-none@example.com","password":"x"}' | python3 -c 'import sys,json;print(json.load(sys.stdin).get("statusCode",""))' 2>/dev/null || true)
  [[ "$sc" =~ ^[A-Z_]+$ ]] && ok "BE 응답 코드 형식 ($sc)" || ng "BE 응답 코드가 enum 이름 형식이 아님 ($sc). 최신 코드로 재시작 필요"
  code=$(curl -s -o /dev/null -w '%{http_code}' "$FE/" || true)
  [[ "$code" == "200" ]] && ok "FE $FE" || ng "FE 응답 없음 ($code). react-boilerplate 에서 npm run dev"
  # MSW 여부는 여기서 확인하지 않는다 (env 파일은 읽기 금지 대상). 첫 시나리오에서 브라우저의 로그인 요청이
  # BE 로 실제로 가는지(MailHog 메일, DB 행 생성 여부)로 확인한다
  code=$(curl -s -o /dev/null -w '%{http_code}' "$MAILHOG/api/v2/messages?limit=1" || true)
  [[ "$code" == "200" ]] && ok "MailHog API" || ng "MailHog 응답 없음"
  return $fail
}

create_user() {
  local email="$1" pw="$2" role="${3:-}"
  [[ "$email" == ${QA_PREFIX}* ]] || { echo "테스트 계정은 $QA_PREFIX 로 시작해야 함: $email" >&2; return 2; }
  local res; res=$(api POST /auth/register "" "{\"email\":\"$email\",\"name\":\"QA\",\"password\":\"$pw\"}")
  [[ "$res" == 200* ]] || { echo "가입 실패: $res" >&2; return 1; }
  local link=""
  for _ in 1 2 3 4 5 6 7 8 9 10; do link=$(mail_link "$email" verify 2>/dev/null) && break; sleep 1; done
  [[ -n "$link" ]] || { echo "인증 메일을 받지 못함" >&2; return 1; }
  local token="${link##*token=}"
  res=$(api GET "/auth/verify-email?token=$token")
  [[ "$res" == 200* ]] || { echo "인증 실패: $res" >&2; return 1; }
  if [[ "$role" == "ADMIN" ]]; then sql "UPDATE member SET role='ROLE_ADMIN' WHERE email='$email'"; fi
  echo "created $email $(sql "SELECT id, role, email_verified FROM member WHERE email='$email'")"
}

make_image() {
  python3 - "$1" <<'PY'
import sys, zlib, struct
w = h = 64
rows = []
for y in range(h):
    row = bytearray([0])
    for x in range(w):
        row += bytes([(x * 4) % 256, (y * 4) % 256, 160])
    rows.append(bytes(row))
data = zlib.compress(b"".join(rows))
def chunk(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + chunk(b"IDAT", data) + chunk(b"IEND", b"")
open(sys.argv[1], "wb").write(png)
print(sys.argv[1])
PY
}

cleanup() {
  local ids; ids=$(sql "SELECT id FROM member WHERE email LIKE '${QA_PREFIX}%@example.com'" | tr '\n' ' ')
  for id in $ids; do redis DEL "APP:AUTH:REFRESH_TOKEN:$id" >/dev/null; done
  sql "DELETE t FROM todo t JOIN member m ON m.id = t.member_id WHERE m.email LIKE '${QA_PREFIX}%@example.com'"
  sql "DELETE FROM member WHERE email LIKE '${QA_PREFIX}%@example.com'"
  echo "cleaned members: ${ids:-none}"
}

cmd="${1:-}"; shift || true
case "$cmd" in
  preflight) preflight ;;
  create-user) create_user "$@" ;;
  mail-link) mail_link "$@" ;;
  mail-count) mail_count "$@" ;;
  login) api POST /auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}" ;;
  api) api "$@" ;;
  sql) sql "$1" ;;
  redis) redis "$@" ;;
  make-image) make_image "$1" ;;
  cleanup) cleanup ;;
  *) sed -n '2,15p' "$0"; exit 2 ;;
esac
