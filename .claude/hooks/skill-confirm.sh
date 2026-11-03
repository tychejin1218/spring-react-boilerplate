#!/bin/bash
# PreToolUse(Skill): 파일을 만들거나 고치는 스킬은 실행 전에 사용자 승인을 받는다.
#
# Claude 가 대화 맥락에 맞는 스킬을 스스로 고를 수 있게 두되, 아래 목록의 스킬은
# Claude Code 가 승인 대화상자를 띄운 뒤에만 실행된다 (permissionDecision: ask).
# 목록에 없는 스킬(/be-check, /fe-check 등)은 그대로 통과한다.
# /qa-scenario 는 SKILL.md 의 disable-model-invocation 으로 직접 호출만 허용하므로 여기에 없다.

set -u

input=$(cat)
skill=$(echo "$input" | jq -r '.tool_input.skill // empty' 2>/dev/null)
[ -z "$skill" ] && exit 0

case "$skill" in
  be-crud|be-test|fe-scaffold|fe-vitest-gen|fe-e2e-gen) ;;
  *) exit 0 ;;
esac

args=$(echo "$input" | jq -r '.tool_input.args // empty' 2>/dev/null)
reason="/$skill 스킬은 파일을 생성하거나 수정합니다. 실행하시겠습니까?"
[ -n "$args" ] && reason="/$skill $args 스킬은 파일을 생성하거나 수정합니다. 실행하시겠습니까?"

jq -n --arg reason "$reason" '{
  hookSpecificOutput: {
    hookEventName: "PreToolUse",
    permissionDecision: "ask",
    permissionDecisionReason: $reason
  }
}'
