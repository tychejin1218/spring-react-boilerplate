#!/bin/bash
# hook 공통 함수. 각 hook 스크립트에서 source 한다.
# 루트에서 시작한 세션과 하위 프로젝트(react-boilerplate/, springboot-boilerplate/)에서
# 시작한 세션 모두에서 동작하도록, 경로는 항상 저장소 루트 기준으로 정규화한다.

# 저장소 루트(절대 경로). git 을 쓸 수 없으면 CLAUDE_PROJECT_DIR 로 대체한다.
hook_repo_root() {
  local start="${CLAUDE_PROJECT_DIR:-$(pwd)}"
  local top
  top=$(git -C "$start" rev-parse --show-toplevel 2>/dev/null)
  echo "${top:-$start}"
}

# stdin JSON에서 tool_input.file_path 를 읽어 저장소 루트 기준 상대 경로로 돌려준다.
# 상대 경로이면 CLAUDE_PROJECT_DIR(세션 시작 디렉토리) 기준으로 절대 경로를 만든 뒤 변환한다.
hook_file_path() {
  local input="$1"
  local file
  file=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)
  [ -z "$file" ] && return 0
  local start="${CLAUDE_PROJECT_DIR:-$(pwd)}"
  case "$file" in
    /*) ;;
    *) file="$start/$file" ;;
  esac
  local root
  root=$(hook_repo_root)
  case "$file" in
    "$root"/*) file="${file#"$root"/}" ;;
  esac
  echo "$file"
}

# 저장소 루트 기준 상대 경로가 어느 프로젝트에 속하는지 반환: fe | be | (빈 문자열)
hook_project_of() {
  case "$1" in
    react-boilerplate/*) echo "fe" ;;
    springboot-boilerplate/*) echo "be" ;;
    *) echo "" ;;
  esac
}
