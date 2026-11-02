#!/bin/bash
# hook 공통 함수. 각 hook 스크립트에서 source 한다.

# stdin JSON에서 tool_input.file_path 를 읽어 프로젝트 루트 기준 상대 경로로 돌려준다.
# 절대 경로이면 CLAUDE_PROJECT_DIR 접두사를 제거한다.
hook_file_path() {
  local input="$1"
  local file
  file=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)
  [ -z "$file" ] && return 0
  local root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
  case "$file" in
    "$root"/*) file="${file#"$root"/}" ;;
  esac
  echo "$file"
}

# 상대 경로가 어느 프로젝트에 속하는지 반환: fe | be | (빈 문자열)
hook_project_of() {
  case "$1" in
    react-boilerplate/*) echo "fe" ;;
    springboot-boilerplate/*) echo "be" ;;
    *) echo "" ;;
  esac
}
