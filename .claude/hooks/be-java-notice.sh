#!/bin/bash
# PostToolUse(Edit|Write): springboot-boilerplate 의 .java 파일 수정 시
# 포맷 정렬과 정적 분석이 필요하다는 컨텍스트를 Claude에 전달한다.

set -u
source "$(dirname "$0")/common.sh"

input=$(cat)
file=$(hook_file_path "$input")
[ -z "$file" ] && exit 0
[ "$(hook_project_of "$file")" = "be" ] || exit 0
case "$file" in *.java) ;; *) exit 0 ;; esac

name=$(basename "$file")
printf '{"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":"[포맷 검증] Java 파일 수정됨: %s. 작업 마무리 시 /be-check 로 포맷 정렬과 checkstyleMain pmdMain 검증이 필요합니다."}}' "$name"
exit 0
