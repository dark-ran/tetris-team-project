# 미사용 import 정리와 최종 검증

작업 ID: `CODE-HYGIENE-01` · 검증일: 2026-10-09.
브랜치: `integration/pr-7-8-11`. 이 기록은 로컬 작업이며 원격 push·Issue 등록·PR 수정은 하지 않았다.

## 변경과 논의 근거

사용자가 IDE의 `GameAction`, `Objects`, `java.awt` 미사용 경고를 정리하도록 요청했다.
JDK 컴파일러로 production·unit test·review integration 소스를 분석해 실제 참조되는 타입과 정적 멤버를 확인했다.
`KeyMapper`의 `java.util.Objects`, `AppApiConnectionTest`의 `tetris.game.GameAction`이 미사용이었다.
두 import가 제거된 상태를 보존하고 관련 파일의 공백·메서드 배치 정리도 함께 반영한다.
`GameScreen` 등에서 실제 사용하는 `GameAction`과 다른 파일의 `Objects` import는 유지한다.

`src/test/java/tetris/quality/ImportAudit.java`와 Gradle `verifyImports`를 추가하여 이후 동일 경고를 검사한다.
주석·문자열을 사용으로 세지 않고, 명시적 import·wildcard·정적 import·상속된 정적 멤버를 해석한다.
미사용 import는 종료 코드 1, 소스 해석 오류는 2로 실패한다.
`check`에 연결했으므로 기존 `build`에서도 실행된다. 별도 라이브러리를 추가하지 않고 Java 21 도구를 사용한다.

## 요구사항·산출물 연결

[요구사항 10~12절](../requirements.md)은 충분한 기능·성능 테스트와 코드·문서·논의 이력의 추적을 요구한다.
이번 변경은 아래처럼 구분하며, 작업 ID는 이 저장소 안에서 사용하는 추적용 식별자다.

| 작업 ID | 결정·요구사항·테스트 기록 | 커밋 범위 |
| --- | --- | --- |
| `GAME-SPEED-01` | [생성 또는 삭제로 가속](gameplay-speed.md) | `d14ce1b` — 속도 규칙·경계·실제 엔진/타이머 테스트 |
| `GAME-PREVIEW-01` | [예시를 반영한 다음 블록 박스](next-piece-preview.md) | `c314f4d` — UI·큐·색각·배치·성능 테스트·검증 이미지 |
| `CODE-HYGIENE-01` | 이 문서와 `ImportAuditTest` | `ca1b64c` — import 정리·검사 도구·검증 기준; 후속 `fix(build)` 커밋 — IDE 소스 연결 수정 |

기능별 Conventional Commits를 사용하며 코드와 해당 테스트·문서를 함께 보관한다.
`git log --oneline -- docs/work-items src/main/java src/test/java src/reviewIntegration/java tools`로 변경 내역을 조회할 수 있다.
UI 이미지는 `docs/images/gameplay-preview-small.png`에 버전 관리하며 나머지 보고서·원시 측정·모드별 이미지는
문서에 적힌 명령으로 `build/` 아래 재생성한다. 로컬 완료와 팀 리뷰·원격 CI·main 반영은 구분한다.

## 최초 검증 결과 (`ca1b64c`)

macOS 26.6.2 · Temurin Java 21.0.12.1 · Gradle 8.14.4.

```sh
./gradlew build jacocoTestReport gameplayPerformanceTest
./gradlew verifyImports
```

| 검사 | 결과 |
| --- | --- |
| 일반 테스트 | 342개 통과, 실패·오류·건너뛰기 0 |
| 실제 모듈 통합 | 1개 통과, 실패·오류·건너뛰기 0 |
| 성능 측정 사례 | 6개 통과, 실패·오류·건너뛰기 0; 입력·화면 그리기 1,800회 |
| 위 자동 검사 합계 | **349개 통과** |
| import 검사 | production·unit·review integration 80개 파일, 미사용 0개 |
| 새 검사 도구 테스트 | 명시적·wildcard·정적·상속·중첩 타입·주석/문자열·한정 이름·해석 오류 등 10개, 일반 테스트 수에 포함 |
| 기존 커버리지 기준 | 보드·블록·엔진 클래스별 줄 커버리지 70% 기준 통과 |
| 수정/추가한 화면·규칙 줄 커버리지 | `SpeedSystem`, `GameScreen`, `NextPiecePreview` 각각 100% |
| 분기 커버리지 | `SpeedSystem`, `NextPiecePreview` 100%; `GameScreen` 18/27 분기 |

보고서: `build/reports/tests/test/`, `reviewIntegrationTest/`, `gameplayPerformanceTest/`,
`build/reports/jacoco/test/html/`. 성능 결과와 측정 범위는 [화면 작업 기록](next-piece-preview.md#성능-검증)에 보관한다.

`./gradlew reviewWindowTest`의 창 검사 **8개는 통과, 기존 포커스 검사 1개는 실패**했다.
첫 보드의 포커스 소유자가 `null`이며, 컴퓨터 제어 도구가 Mac 잠금 상태를 확인했다.
잠금 해제 후 해당 검사와 실제 키 입력을 재확인해야 한다. 테스트를 건너뛰거나 성공으로 바꾸지 않았다.
Windows·원격 CI·과제 최소 사양 검증은 이번 로컬 결과에 포함되지 않는다.

## IDE의 `ImportAudit cannot be resolved` 수정

사용자가 해당 오류를 보고했다. 기존 도구는 `tetris.quality` 패키지를 선언하면서
`tools/import-check/ImportAudit.java`에 있었고, 별도 Gradle 소스 세트의 출력을 테스트 경로에 연결했다.
Gradle에서는 컴파일됐지만 IDE가 만든 `ImportAuditTest.class`에는 실제 미해결 타입 오류가 남아 있었다.

도구를 패키지와 일치하는 표준 테스트 경로 `src/test/java/tetris/quality/ImportAudit.java`로 옮겼다.
별도 `importCheck` 소스 세트와 경로 추가 설정을 제거하고, `verifyImports`는 `testClasses`에 의존하며
표준 테스트 실행 경로를 사용하도록 변경했다. 도구의 검사 동작은 그대로이며 배포용 JAR에는 포함되지 않는다.

```sh
./gradlew clean build jacocoTestReport
```

- 이전 Gradle 빌드 결과를 제거한 뒤 일반 테스트 342개와 실제 모듈 통합 테스트 1개, **총 343개 통과**.
- 실패·오류·건너뛰기 0개. 기존 클래스별 줄 커버리지 기준 통과.
- 검사 도구 자신을 포함한 소스 81개 파일에서 미사용 import 0개.
- IDE의 Java 프로젝트 설정을 갱신한 뒤 문제 패널에서 오류·경고 0개 확인.
- 이번 경로 수정에서는 성능·실제 창 검사를 다시 실행하지 않았다. 해당 결과와 한계는 위 최초 검증 기록에 남긴다.
- 로컬 커밋으로 기록하며 원격에는 push하지 않는다.

## 원격 UI 변경 병합 후 검증 (2026-10-10)

PR #14의 원격 UI 수정 2개와 로컬 변경을 함께 반영한 뒤 전체 362개 검사를 통과했다.
이전 잠금 상태에서 실패했던 포커스 검사도 포함해 실제 창 검사 9개가 모두 통과했다.
충돌 해결·추가 회귀 테스트·실행 범위는 [원격 UI 수정 통합 기록](pr14-ui-sync.md)에 보관한다.
