#!/bin/bash
# PreToolUse(Bash): git commit 직전, 스테이징된 변경에 springboot-boilerplate 가 포함되면
# Checkstyle + PMD 를 실행하고 실패 시 커밋을 차단한다(exit 2).

set -u
source "$(dirname "$0")/common.sh"

input=$(cat)
cmd=$(echo "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)
[ -z "$cmd" ] && exit 0
echo "$cmd" | grep -q 'git commit' || exit 0

root=$(hook_repo_root)
be_dir="$root/springboot-boilerplate"

staged=$(git -C "$root" diff --cached --name-only 2>/dev/null)
echo "$staged" | grep -q '^springboot-boilerplate/' || exit 0

if [ ! -x "$be_dir/gradlew" ]; then
  echo "[be-commit-gate] springboot-boilerplate/gradlew 를 찾을 수 없어 정적 분석을 실행하지 못했습니다." >&2
  exit 2
fi

if (cd "$be_dir" && ./gradlew -q checkstyleMain pmdMain); then
  exit 0
fi

echo "정적 분석 실패: springboot-boilerplate 에서 ./gradlew checkstyleMain pmdMain 오류를 수정한 뒤 다시 커밋하세요." >&2
exit 2
