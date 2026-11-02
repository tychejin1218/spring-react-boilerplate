#!/bin/bash
# PreToolUse(Edit|Write|MultiEdit): 에이전트 팀 팀원의 수정 범위를 강제한다.
#
# hook 입력의 agent_type(팀원을 생성할 때 지정한 에이전트 정의 이름)이
#   fe-* 이면 react-boilerplate/ 밖의 파일 수정을 차단
#   be-* 이면 springboot-boilerplate/ 밖의 파일 수정을 차단 (단, 그 디렉토리 안의 docker-compose.yml 포함)
# agent_type 이 없거나(리드 세션, 일반 세션) 접두사가 다르면 아무것도 하지 않는다.
#
# 팀원을 만들 때는 반드시 fe-* / be-* 에이전트 정의(.claude/agents/)를 지정해야 이 guard 가 동작한다.

set -u
source "$(dirname "$0")/common.sh"

input=$(cat)
agent_type=$(echo "$input" | jq -r '.agent_type // empty' 2>/dev/null)
[ -z "$agent_type" ] && exit 0

case "$agent_type" in
  fe-*) scope="fe"; allowed="react-boilerplate/" ;;
  be-*) scope="be"; allowed="springboot-boilerplate/" ;;
  *) exit 0 ;;
esac

file=$(hook_file_path "$input")
[ -z "$file" ] && exit 0

if [ "$(hook_project_of "$file")" != "$scope" ]; then
  echo "[team-scope-guard] 에이전트 '$agent_type' 는 $allowed 하위 파일만 수정할 수 있습니다. 대상: $file" >&2
  exit 2
fi
exit 0
