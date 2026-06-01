---
name: feedback
description: 작업 방식 피드백 및 주의 사항
metadata:
  type: feedback
---

## 브랜치 전략
모든 작업을 feature/performance 하나에 축적 (Day 3~7 포함).
**Why:** 사용자가 빅 PR 전략을 선택. 기능별 브랜치 분기 불필요.
**How to apply:** 새 기능도 feature/performance에서 작업. 브랜치 생성 제안 불필요.

## 의사결정 자동 기록
decisions/ 폴더에 항상 자동 기록. "기록할까요?" 묻지 말 것.
**Why:** 핸드오프 문서 규칙 (always-on memory rules).
**How to apply:** 아키텍처 결정이 있을 때마다 decisions/ 파일 작성.

## 세션 시작 루틴
핸드오프 문서(/Users/sylee/Documents/concert-reservation-portfolio/)를 읽고 현황 파악 후 작업.
git status + git log로 커밋 상태 확인 후 다음 Day 작업 시작.
