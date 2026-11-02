#!/bin/bash
# PostToolUse(Edit|Write): react-boilerplate 소스 파일에만 Prettier + ESLint --fix 적용.
# 다른 프로젝트 파일이나 비대상 확장자는 아무것도 하지 않는다.

set -u
source "$(dirname "$0")/common.sh"

input=$(cat)
file=$(hook_file_path "$input")
[ -z "$file" ] && exit 0
[ "$(hook_project_of "$file")" = "fe" ] || exit 0

case "$file" in
  *.ts|*.tsx|*.js|*.jsx|*.css|*.json|*.md) ;;
  *) exit 0 ;;
esac

root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
fe_dir="$root/react-boilerplate"
rel="${file#react-boilerplate/}"

if [ ! -d "$fe_dir/node_modules" ]; then
  echo "[fe-format] react-boilerplate/node_modules 없음. npm install 후 포맷 hook이 동작합니다." >&2
  exit 0
fi

cd "$fe_dir" || exit 0
npx prettier --write "$rel" >/dev/null 2>&1
case "$rel" in
  *.ts|*.tsx|*.js|*.jsx) npx eslint --fix "$rel" >/dev/null 2>&1 ;;
esac
exit 0
