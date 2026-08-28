#!/bin/bash
# Claude Edit/Write 이후 네이티브 소스의 공백 오류를 빠르게 확인합니다.

EVENT=$(cat)
FILE_PATH=$(printf '%s' "$EVENT" | jq -r '.tool_input.file_path // empty' | tr -d '\r')
[ -z "$FILE_PATH" ] && exit 0

case "$FILE_PATH" in
  "$CLAUDE_PROJECT_DIR"/*.kts|"$CLAUDE_PROJECT_DIR"/**/*.kt|"$CLAUDE_PROJECT_DIR"/**/*.xml)
    git -C "$CLAUDE_PROJECT_DIR" diff --check -- "$FILE_PATH" >/dev/null || exit 2
    ;;
esac
exit 0
