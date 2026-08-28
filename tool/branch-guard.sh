#!/bin/bash
# AlbumFrame 작업 컨텍스트에서 보호 브랜치 직접 commit을 차단합니다.

CMD=$(jq -r '.tool_input.command // empty' | tr -d '\r')
[ -z "$CMD" ] && exit 0
echo "$CMD" | grep -qE 'git[[:space:]].*commit' || exit 0

REPO_ROOT=$(git -C "$CLAUDE_PROJECT_DIR" rev-parse --show-toplevel 2>/dev/null)
[ -z "$REPO_ROOT" ] && exit 0
BRANCH=$(git -C "$REPO_ROOT" branch --show-current 2>/dev/null)

case "$BRANCH" in
  main|dev|release)
    echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"보호 브랜치 직접 commit은 차단됩니다. feature 브랜치에서 작업해 주세요."}}'
    ;;
esac
exit 0
