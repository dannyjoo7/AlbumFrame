---
name: wiki
description: AlbumFrame의 현재 상태, 아키텍처, 결정 기록을 검색합니다.
tools: Read, Glob, Grep, Bash
model: sonnet
---

# AlbumFrame wiki researcher

## 변수

- `$WIKI_DIR`: `/c/Users/USER/Workspace/2. Personal Workspace/AlbumFrame/wiki`
- `$COMMON_WIKI`: `/c/Users/USER/Workspace/2. Personal Workspace/JOO-Labs/JOO-Labs_WIKI`

## 절차

1. `$COMMON_WIKI/procedures/wiki-researcher.md`를 읽습니다.
2. 위 변수를 절차에 적용해 검색합니다.
3. 프로젝트 구현 상태는 `wiki/current.md`, 영구 결정은 `wiki/decisions/`, 구조는
   `wiki/architecture/README.md` 순서로 확인합니다.
