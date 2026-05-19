# AGENTS.md

## 프로젝트 개요

MoNew는 Spring Boot 기반 뉴스 기사 관리 및 수집 백엔드 프로젝트입니다.

주요 기능:
- 뉴스 기사 수집
- 기사 목록 조회
- 관심사 기반 필터링
- 댓글 및 조회 여부 집계
- 알림 생성

기술 스택:
- Java 17
- Spring Boot
- QueryDSL
- PostgreSQL
- MongoDB
- AWS S3
- Gradle

---

# 아키텍처 규칙

- 패키지 구조는 도메인형 구조를 사용한다.
    - `com.springboot.monew.{domain}`

- 기존 패키지 구조와 계층 구조를 우선 따른다.
- 불필요한 레이어 추가를 지양한다.
- 요구사항과 관계없는 리팩토링은 하지 않는다.

---

# 코드 컨벤션

## 클래스
- PascalCase 사용

## 변수 / 메서드
- camelCase 사용

## 상수
- UPPER_SNAKE_CASE 사용

---

# 메서드 명 규칙

Entity 이름을 메서드명에 중복해서 붙이지 않는다.

좋은 예시:
- `create()`
- `find()`
- `findAll()`
- `update()`
- `delete()`
- `findByUserId()`

좋지 않은 예시:
- `createUser()`
- `deleteArticle()`

추가 기능 메서드는 기존 프로젝트 네이밍 스타일을 우선 참고한다.

---

# 구현 규칙

- 기존 프로젝트 코드 스타일을 우선 따른다.
- 최소 변경 원칙을 지킨다.
- DTO / Entity / Repository / Test 간 영향 범위를 함께 고려한다.
- 기존 API 응답 구조를 임의로 변경하지 않는다.
- 기존 Exception / ErrorCode 구조를 유지한다.
- 의미 없는 변수명 축약을 지양한다.
- 주석은 로직 흐름 기준으로 작성한다.

---

# QueryDSL 및 성능 규칙

뉴스 기사 목록 조회는 성능 민감 영역이다.

다음 사항을 항상 고려한다:
- N+1 문제
- 불필요한 fetch join
- sorting 비용
- GroupAggregate 비용
- index 사용 여부
- cursor pagination 구조

기본 기사 조회 조건:
- `is_deleted = false`
- source = `NAVER`
- 정렬:
    - `published_at DESC`
    - `created_at DESC`

기존 QueryDSL 패턴을 우선 사용한다.

---

# 보안 규칙

- Secret Key / AWS Key / DB 계정 정보를 하드코딩하지 않는다.
- Spring Security 인증 흐름을 고려한다.
- 사용자 식별은 다음 헤더 기준 흐름을 따른다.
    - `Monew-Request-User-ID`

---

# 테스트 규칙

테스트 위치:
- production 코드와 동일한 패키지 구조 사용

테스트 파일명:
- `{대상클래스}Test`

테스트 규칙:
- `@DisplayName` 적극 사용
- 한글 서술형 테스트명 사용
- 외부 의존성은 Mock 처리
- 테스트를 통과시키기 위해 기대값을 임의 수정하지 않는다

가능하면 작업 후 아래 명령어를 실행한다:

```bash
./gradlew test
```

특정 테스트 실행:

```bash
./gradlew test --tests "클래스명"
```

---

# Git 규칙

브랜치 전략:
- `main`
    - 운영 배포용
    - 직접 push 금지
- `dev`
    - 개발 통합 브랜치
- `feature/#이슈번호/task`
- `fix/#이슈번호/task`

feature 브랜치는 merge 후 삭제한다.

---

# 커밋 규칙

커밋 형식:

```text
type: subject
```

사용 가능한 타입:
- feat
- fix
- docs
- style
- refactor
- perf
- test
- chore

예시:
```text
feat: 기사 목록 조회 API 추가
fix: 뉴스 조회 정렬 오류 수정
```

---

# PR 규칙

PR 제목 형식:

```text
[type] 작업 내용
```

예시:
```text
[feat] 기사 관심사 등록 기능 구현
```

PR 본문에는 반드시 포함:
- 작업 내용
- 변경 사항
- 주요 설계 의도
- 암묵적 메서드 네이밍 사용 시 설명

가능하면 작은 단위 PR을 유지한다.

---

# 리뷰 Agent 규칙

리뷰 시 다음 사항을 우선 확인한다:
- 버그 가능성
- 성능 문제
- 트랜잭션 문제
- QueryDSL 문제
- N+1 위험
- 보안 문제
- 테스트 누락

리뷰는 다음 기준으로 분류한다:
- Critical
- Major
- Minor

요청받지 않은 대규모 리팩토링은 제안하지 않는다.

---

# 개발 Agent 규칙

구현 시 다음 순서를 따른다:

1. 관련 파일 먼저 분석
2. 기존 패턴 파악
3. 최소 변경으로 구현
4. 현재 아키텍처 유지
5. 변경 파일과 이유 설명

작업 완료 시 반드시 정리:
- 변경 파일
- 변경 이유
- 테스트 실행 여부
- 남은 리스크

---

# 금지 사항

- unrelated refactoring 금지
- 사용하지 않는 코드 대량 추가 금지
- 기존 API Spec 임의 변경 금지
- 테스트 삭제 금지
- 설정값 하드코딩 금지
- main 브랜치 직접 수정 금지