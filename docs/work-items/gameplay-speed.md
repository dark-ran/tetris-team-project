# 블록 생성·줄 삭제에 따른 낙하 속도

작업 ID: `GAME-SPEED-01` · 검증일: 2026-10-09.
브랜치: `integration/pr-7-8-11` · 관련 PR: [#14](https://github.com/dark-ran/tetris-team-project/pull/14).
외부 Issue는 등록하지 않았다. 이 문서와 기능별 Git 커밋으로 요구사항·결정·코드·테스트를 연결한다.
이 변경은 로컬 검증 완료 상태이며 원격 PR에는 아직 포함되지 않았다.

## 요구사항과 수정 이유

[요구사항 4절](../requirements.md#4-게임-조작)은 일정 수의 블록 생성 **또는** 줄 삭제 후 가속을 요구한다.
기존 `SpeedSystem`은 생성 수를 인자로 받아도 무시하여, 줄을 삭제하지 않는 플레이에서는 가속하지 않았다.
기존 테스트에도 이 누락을 정상으로 기대하는 단언이 있어 함께 수정했다.

## 적용 기준과 논의사항

구체적인 생성 수는 요구사항에 정해져 있지 않다. 이번 구현에서는 기존 10줄 기준과 같은
**10개 생성**을 기본값으로 정했다. 팀 합의 완료로 간주하지 않으며, 검토 시 난이도 조정 대상이다.
기준은 `SpeedSystem.PIECES_PER_LEVEL`, `ROWS_PER_LEVEL`, `INTERVAL_DECREASE_MILLIS`에 모았다.

- 첫 생성 성공도 포함한다. 새 게임은 생성 1개·삭제 0줄·레벨 1·1,000ms로 시작한다.
- 레벨 = `1 + max(생성 수 / 10, 삭제 줄 수 / 10)`이며 정수 나눗셈을 사용한다.
- 레벨마다 100ms 줄여 10개/10줄이면 900ms, 20개/20줄이면 800ms, 최소 간격은 100ms이다.
- 두 기준을 동시에 달성해도 중복 가산하지 않는다. 생성 실패는 엔진의 생성 성공 수에 포함되지 않는다.
- 경계를 넘긴 행동의 점수는 그 행동이 시작할 때의 속도로 계산한다. 다음 낙하부터 기존 가속 보너스를 적용한다.
- 메뉴·설정에서는 진행을 멈추고 복귀하면 속도를 유지한다. 새 게임은 누적 수치·속도를 초기화한다.

## 추적과 완료 기준

| 요구 동작 | 구현 | 검증 |
| --- | --- | --- |
| 생성 또는 삭제로 가속, 경계·동시 달성·최소 간격·잘못된 입력 | `rule/SpeedSystem.java` | `RuleSystemsTest.eitherCounterIncreasesSpeedWithoutDoubleCounting`, `negativeCountersAreRejected` |
| 줄을 지우지 않아도 생성 수로 가속, 실제 점수·레벨·재시작 반영 | 기존 `app/AppController.java`의 `updateProgress` 연결 | `AppIntegrationTest.spawningWithoutClearingUpdatesIntervalAndOnlySubsequentDropsGetTheBonus` |
| 실제 삭제 수·생성 수의 반복 증가와 후속 점수 | 실제 엔진의 누적 수치 → 앱 제어 | `AppIntegrationTest.clearedRowsAndSpawnsUpdateLevelIntervalAndNextDropBonus` |
| 실행 중 타이머 간격 반영, 반복 요청으로 낙하가 지연되지 않음 | 기존 `loop/GameLoop.java` | `GameLoopTest.acceleratedTimerKeepsFallingDuringRepeatedUnchangedIntervalRequests` 및 기존 정지·재개 테스트 |

공개 메서드 이름과 엔진 연결 API는 유지했다. 테스트를 위한 엔진 상태 변경 API는 추가하지 않았다.
호출 순서와 수치의 의미는 [엔진 계약](../contracts/game-engine.md)에 정리되어 있다.

## 검증 결과

macOS · Temurin Java 21.0.12.1 · Gradle 8.14.4에서 `./gradlew build jacocoTestReport` 실행.
일반 테스트 328개 + 모듈 통합 1개 = **329개 통과**, 실패·오류·건너뛰기 0.
`SpeedSystem` 줄·분기 커버리지 100%, 기존 보드·블록·엔진 클래스별 70% 기준 통과.
실제 Swing 타이머에 10ms마다 동일 간격을 요청하면서 100ms 낙하 3회를 2초 안에 수신했다.
이는 반복 요청에 따른 타이머 지연 회귀 검사이며 최소 사양 기기의 성능 인증은 아니다.

재생성 가능한 결과: `build/reports/tests/test/index.html`,
`build/reports/tests/reviewIntegrationTest/index.html`, `build/reports/jacoco/test/html/index.html`.
후속 미리보기 변경과 함께 실제 창·성능 검증 결과를 추가 기록한다. 원격 CI와 타 OS 검증은 미실행이다.
