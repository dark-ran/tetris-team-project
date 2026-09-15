# Architecture Proposal

> 이 문서는 프로젝트 구조에 대한 **초안/제안안**입니다.
> 확정된 요구사항이 아니며 팀원 의견에 따라 변경할 수 있습니다.

## 1. 전체 구조 제안

```text
TetrisTeamProject/
│
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── package-windows.yml
│
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── development.md
│   └── decisions/
│
├── packaging/
│   └── windows/
│
├── src/main/java/tetris/
│   ├── Main.java
│   ├── app/
│   ├── board/
│   ├── piece/
│   ├── game/
│   ├── rule/
│   ├── loop/
│   ├── input/
│   ├── settings/
│   ├── scoreboard/
│   └── ui/
│
└── src/test/java/tetris/
    ├── board/
    ├── piece/
    ├── game/
    ├── rule/
    ├── loop/
    ├── input/
    ├── settings/
    ├── scoreboard/
    └── ui/
```

## 2. 패키지별 역할 제안

| 패키지 | 역할 |
|---|---|
| `app` | 애플리케이션 시작, 화면 전환 등 |
| `board` | Board 상태, 충돌, Line Clear |
| `piece` | Tetromino, 회전, 생성, Next Queue |
| `game` | GameEngine, GameState, GameAction 등 게임 핵심 흐름 |
| `rule` | 점수, 속도 증가 등 게임 규칙 |
| `loop` | Tick 및 Game Loop |
| `input` | 키 입력, 사용자 키 설정 반영 |
| `settings` | 설정 데이터, 저장/복원 |
| `scoreboard` | 점수 기록, 정렬, 저장/복원 |
| `ui` | 시작 메뉴, 게임 화면, 설정, 스코어보드, 게임 오버 화면 |

## 3. 게임 호출 구조 제안

```text
Keyboard
   ↓
InputHandler
   ↓
KeyMapper
   ↓
GameAction
   ↓
GameController
   ↓
GameEngine
   ↓
GameState
   │
   ├── Board
   ├── Piece
   ├── NextPieceQueue
   ├── ScoreSystem
   └── SpeedSystem
   ↓
GameSnapshot
   ↓
GameScreen
```

이 구조는 UI와 게임 핵심 로직을 직접 결합하지 않고 분리하기 위한 제안이다.

## 4. 설정 구조 제안

```text
SettingsScreen
      ↓
SettingsService
      ↓
SettingsRepository
```

- `SettingsScreen`: 설정 UI
- `SettingsService`: 설정 변경/검증
- `SettingsRepository`: 파일 저장 및 복원

## 5. 스코어보드 구조 제안

```text
GameOverScreen
      ↓
ScoreBoardService
      ↓
ScoreRepository
```

- `GameOverScreen`: 이름 입력 및 게임 종료 UI
- `ScoreBoardService`: 점수 등록, 순위 계산, Top 기록 관리
- `ScoreRepository`: 파일 저장 및 복원

## 6. 상태 관리 제안

화면 전환과 게임 진행 상태는 FSM(Finite State Machine) 형태로 관리하는 방안을 검토한다.

예:

```text
AppState
- START_MENU
- GAME
- SETTINGS
- SCOREBOARD
- EXIT

GamePhase
- RUNNING
- PAUSED
- GAME_OVER
```

이 부분은 확정사항이 아니며 실제 구현 규모를 보고 단순화할 수 있다.

## 7. 역할 분담 초안

| 구분 | 핵심 영역 | 주요 패키지 |
|---|---|---|
| A | Board / Tetromino | `board`, `piece` |
| B | Game Engine / Rules | `game`, `rule`, `loop` |
| C | UI / Input | `ui`, `input` |
| D | Application / Settings / ScoreBoard / Build | `app`, `settings`, `scoreboard`, Build/CI/배포 |

- 실제 담당자는 팀 논의 후 결정한다.
- 각 담당자는 자기 기능에 대한 테스트도 함께 작성한다.
- 다른 담당자의 영역 수정이 필요한 경우 먼저 Issue 또는 팀 논의를 통해 공유한다.

## 8. 구조 관련 논의 필요 사항

- `GameController`와 `GameEngine`을 별도로 둘지
- `GameSnapshot`이 필요한지
- Settings/ScoreBoard에 Service/Repository 계층을 모두 둘지
- FSM을 사용할지
- 패키지를 현재 수준으로 세분화할지 일부 통합할지
- 각 팀원의 실제 담당 영역
