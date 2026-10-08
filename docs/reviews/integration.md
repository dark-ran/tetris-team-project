# PR #7·#8·#11 통합

통합 브랜치: `integration/pr-7-8-11`.
시작점: main의 `48cb19a102460b656be07282a745ff7efdf83e9e`.
main으로의 반영은 이 브랜치의 별도 PR에서 검토한다.

## 병합 기록

기존 PR의 대상 브랜치를 통합 브랜치로 변경한 뒤 merge 방식으로 병합했다.
각 PR은 GitHub에서 병합됨으로 닫혔다.

| PR | 반영한 head | 병합 커밋 |
| --- | --- | --- |
| [#7](https://github.com/dark-ran/tetris-team-project/pull/7) | 9f8d815 | 8b289404414cbc484c912110aec3185e01ef1188 |
| [#8](https://github.com/dark-ran/tetris-team-project/pull/8) | 46c27e4 | 2fb18c8552241a8f3019770290c6910d42118d5a |
| [#11](https://github.com/dark-ran/tetris-team-project/pull/11) | 437dfbf | 292387859bbad55c2d535c35f36157d0243a19d2 |

#11의 `437dfbf`는 #7·#8이 포함된 통합 브랜치를 가져와 충돌을 해결한 merge 커밋이다.
다른 개발자의 이력을 덮어쓰지 않고 일반 push로 반영했다.

## 충돌 해결과 연결 계약

총 9개 파일의 충돌을 다음 기준으로 해결했다.

| 담당 구현 | 유지한 파일 |
| --- | --- |
| #7 설정·입력·타이머 | GameSettings, InputHandler, KeyMapper, GameLoop |
| #8 화면·메뉴·편집 UI | GameScreen, SettingsScreen, GameOverScreen, ScoreboardScreen |
| #11 앱 수명주기 | AppController |

AppState와 앱 테스트는 #11의 게임 제어 흐름을 유지했다.
해결 후 코드와 테스트는 기존에 280개 검사를 통과한 통합본과 동일함을 확인했다.

- 입력 매핑·루프 제어·렌더링의 기존 주요 메서드 이름을 유지했다.
- 메뉴 표시와 설정 복귀 메서드를 추가했다.
- SettingsScreen.Values를 setOnSaveValues로 전달하고 AppController가 검증된 GameSettings를 만든다.
- 설정·게임 메뉴에서는 엔진과 자동 낙하를 멈춘 채 진행 상태를 유지한다.
- 게임 복귀를 선택할 때만 재개하며 변경한 키·화면 크기·구분 문자 설정을 적용한다.
- 게임오버에서는 최종 점수·랭킹 등록 절차를 유지하고 Space로 새 게임을 시작한다.

## 자동 검증

Java 21 / macOS에서 다음 명령으로 검증했다.

```sh
./gradlew clean build jacocoTestReport reviewWindowTest
```

| 검사 | 개수 | 실패·오류·건너뛰기 |
| --- | --- | --- |
| 일반 테스트 | 278 | 0 |
| 실제 모듈 통합 테스트 | 1 | 0 |
| 실제 창의 포커스·Swing 키 이벤트 테스트 | 1 | 0 |
| 합계 | 280 | 0 |

기존 보드·블록 및 엔진 클래스별 JaCoCo 기준 검사도 통과했다.
#11 충돌 해결 커밋은 Windows·macOS·Ubuntu GitHub CI도 모두 통과한 후 병합했다.

reviewIntegrationTest를 check에 연결했다.
GitHub CI의 기존 clean build jacocoTestReport 명령에서도 일반 테스트와 함께
실제 저장소·키 매핑·타이머·엔진·화면을 연결한 통합 검사를 수행한다.
설정 저장 → 메뉴 복귀 → 동일 게임 재개와 게임오버 → Space 재시작을 검사한다.
창이 필요한 reviewWindowTest는 GUI를 사용할 수 있는 환경에서 별도로 실행한다.

창 검사는 실제 포커스 소유자에게 Swing 키 이벤트를 전달한다.
이름 입력란의 공백 입력, 설정 화면의 Esc 복귀, 다른 버튼에 포커스가 있는 상태의
Space 재시작과 창 닫기를 검사한다.
외부에서 OS 키를 주입하는 검증은 이 Mac의 Java 창에서 완료하지 못했다.

## 범위

기존 작업 폴더의 미커밋 변경은 그대로 보존했다.
새 PR #13의 CI·개발 환경 변경은 별도 PR로 유지한다.
main에 직접 push하거나 병합하지 않았다.
