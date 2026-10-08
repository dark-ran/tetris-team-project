# 게임오버 이름 입력 화면

작업 브랜치: `integration/pr-7-8-11`.
검증일: 2026-10-08.

## 변경

기존 세로 배치가 이름 입력란을 남은 높이까지 늘리면서 작은 글자가 큰 흰색 박스 안에 표시됐다.
입력란을 게임과 같은 어두운 배경·밝은 글자·노란 커서로 변경했다.
글자는 20pt이며, 입력란은 너비 440과 내용에 맞춘 높이로 유지한다.
세 가지 창 크기에서 20자 한글 이름이 입력 영역에 들어가며, 등록·건너뛰기·재시도 버튼도 창 안에 표시한다.

`GameOverScreen` 생성자는 화면을 조립하는 역할만 갖도록 정리했다.

| 역할 | 메서드 |
| --- | --- |
| 입력란 스타일·크기 | `configureNameField` |
| 결과 카드 | `createResultCard` |
| 이름 입력과 오류 영역 | `createRegistrationForm` |
| 등록·건너뛰기·재시도 버튼 | `createRegistrationActions` |
| 화면 하단 이동 버튼 | `createNavigation` |
| 등록·재시도 처리 | `registerName`, `skipRegistration`, `retryRegistration` |
| 단축키 설정 | `configureShortcuts` |
| 점수·등록 가능 여부·입력값 갱신 | `showResult` |

공개 콜백과 `showScreen` 메서드의 연결 방법은 유지한다.
새 결과를 표시할 때 이전 이름과 오류 표시를 정리하며, 저장 실패 시에는 현재 입력을 보존한다.
이름 입력란의 공백 입력과 다른 위치의 Space 재시작, Esc 메뉴 이동도 유지한다.

## 검증

Java 21 / macOS에서 다음 명령으로 검증한다.

```sh
./gradlew build jacocoTestReport reviewWindowTest
```

전체 310개 검사(일반 303, 실제 모듈 통합 1, 실제 창 6)를 통과했다.
기존 보드·블록·게임 엔진의 커버리지 기준 검사도 통과했다.
추가 검사는 세 프리셋의 입력란 비율·한글 20자 표시·어두운 배경·버튼 배치와
이름 검증·저장 재시도·등록 영역 표시 상태를 다룬다.
실제 창에서는 이름에 공백을 입력해도 재시작하지 않는 것을 재검증했다.
확인 이미지는 `build/review-game-over-registration.png`에 생성한다.

사용자 요청에 따라 이 작업은 먼저 별도 로컬 커밋으로 기록한다.
스코어보드 초기화 연결은 이후 별도 작업으로 진행하며, 원격에는 push하지 않는다.
