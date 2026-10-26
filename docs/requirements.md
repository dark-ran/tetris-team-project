# 요구사항과 구현·검증 연결

기본 게임(Req1) 위에 난이도·아이템·줄 삭제 연출(Req2)을 연결했다. 현재 규칙의 수치와 사용 방법은 [게임 안내](game-guide.md), 클래스의 책임과 확장 방법은 [구조 설명](architecture.md), 측정 결과는 [검증 결과](verification.md)에 정리한다.

## Req1 · 기본 게임

| ID | 요구 내용 | 구현과 확인 방법 |
|---|---|---|
| R1-01 | 게임 이름, 시작·설정·점수판·종료 메뉴, 키보드 이동·선택 | StartMenu, SwingKeyBindings. MenuSettingsRestartTest·ReviewWindowTest |
| R1-02 | 20×10 보드와 7종 기본 블록, 균등 선택 | Board, Tetromino, PieceQueue. NORMAL에서 각 1/7. BoardTest·TetrominoTest·DifficultyGenerationTest |
| R1-03 | 다음 블록과 실시간 점수 | GameScreen·NextPiecePreview가 같은 GameState를 사용. EngineBoardRenderingTest·AppApiConnectionTest |
| R1-04 | 초기 1초 낙하, 진행에 따른 가속 | GameLoop·SpeedSystem. 생성 수·삭제 수 중 높은 진행값 사용. DifficultySpeedTest·ReviewWindowTest |
| R1-05 | 좌우·아래 이동, 90도 회전, 즉시 낙하, 충돌 방지 | GameEngine·PieceMovement. GameEngineTest·GameMenuFlowTest |
| R1-06 | 반복 입력, 일시정지·재개, 게임 내부 종료 | InputHandler·GameMenuOverlay·AppController. 입력·루프 테스트와 실제 창 테스트 |
| R1-07 | 서로 다른 색, 유사 모양 구분, 색각 대응 | 색각 모드 5종과 독립 패턴, 대비되는 아이템 문자. ColorVisionRenderingTest 및 아이템별 RenderingTest |
| R1-08 | 칸당 점수, 가속 추가 점수, 자체 추가 점수 | 기본 1점·가속 2점, 1~4줄 100/300/500/800점. RuleSystemsTest·GoldenScoreTest |
| R1-09 | 크기 3종, 키 설정, 점수 초기화, 기본값, 설정 유지 | SettingsScreen·SettingsService·SettingsRepository. SettingsRecoveryTest·WindowSizeSettingsTest·ScoreboardResetTest |
| R1-10 | 게임오버, 이름 등록, 새 기록 강조, 메뉴 복귀 | AppController·GameOverScreen·ScoreboardScreen. AppControllerTest·GameOverRegistrationTest·ModeScoreboardScreenTest |
| R1-11 | 점수 내림차순 상위 10개, 종료 후 유지 | 모드별 Top 10, 동점은 기존 기록 우선. ScoreBoardTest·ModeScoreBoardTest·DifficultyScoreBoardTest |
| R1-12 | Java 21, 지원 OS, 성능·용량 | Java 21로 빌드, macOS 26.6.2 arm64에서 실행 확인. 256MiB 힙 성능 검사 6건. Windows 자동 빌드·테스트·패키지 확인 |
| R1-13 | VCS, 이슈와 업무 이력 | GitHub Issue·기능별 PR·커밋, [작업 현황](req2-team-plan.md) |
| R1-14 | 단위·비기능 검사, 커버리지, 반복 실행 체계 | JUnit 5·JaCoCo·3 OS CI. 단위 541개, 줄 93.50%. build에서 전체 50%와 핵심 클래스별 70% 검사 |
| R1-15 | 아이콘과 더블클릭 실행파일, 재생성 체계 | Gradle packageMacos·packageWindows, Java 동봉. [배포 안내](distribution.md) |

화면은 Swing으로 구성하고 보드의 칸·아이템 문자를 직접 그린다. 실행 환경은 macOS를 실기 검증 대상으로 삼았다. Windows는 추가 배포 대상이며 실제 PC 조작 확인은 별도로 기록한다. 256MiB 힙 측정은 1GB 물리 RAM 기기의 직접 검증과 구분한다.

## Req2 · 난이도와 아이템

| ID | 요구 내용 | 구현과 확인 방법 | 작업 |
|---|---|---|---|
| R2-01 | EASY·NORMAL·HARD 설정과 저장 | Difficulty·GameSettings, 다음 게임부터 적용. DifficultySettingsTest·DifficultyFlowTest | #22, #28 |
| R2-02 | I 가중치 12/10/8, 나머지 각 10 | PieceQueue의 누적 가중치 추첨. 모든 추첨 구간과 실제 생성 분포 검사 | #22, #26 |
| R2-03 | 난이도에 따른 속도 증가량 ±20% | 레벨당 80/100/120ms 감소, 최소 100ms. DifficultySpeedTest | #22 |
| R2-04 | 점수 기록에서 획득 난이도 구분 | 파일 v3와 표의 난이도 열, 구형 파일 호환. DifficultyScoreBoardTest·ModeScoreboardScreenTest | #28, #29 |
| R2-05 | 삭제되는 줄의 애니메이션 | 350ms 반짝임·400ms 진행 보류, 화면 이탈·재시작 정리, G 문자 보존. BoardPreviewAnimationTest | #24, #25 |
| R2-06 | 실제 생성 코드 1,000회 이상·오차 ±5% | 난이도별 100,000회에서 기대 횟수 대비 상대오차 ≤5%, 별도 1,000회 보조 검사 | #26 |
| R2-07 | 시작 메뉴의 아이템 모드와 모드별 기록 | 두 시작 경로, 모드별 Top 10. ItemModeConnectionTest·ModeScoreBoardTest | #21, #28 |
| R2-08 | 매 10줄 보상, 새 미리보기와 실제 생성 일치 | 누적 줄 수·예약 아이템·Tetromino 전체 정보 전달. 아이템별 ConnectionTest | #21, #23, #28 |
| R2-09 | L의 무작위 부착, 고정 행 삭제, 일반 점수 | 네 위치 각 25%, 회전 반영, 빈칸이 있어도 해당 행 삭제. LineClearItemEngineTest | #21 |
| R2-10 | 너비 4 무게추, 아래 제거, 첫 접촉 후 좌우 금지 | 6칸 W, 고정 후 소멸·점수 0. WeightItemEngineTest·WeightItemConnectionTest | #21 |
| R2-11 | 자체 아이템 최소 3종 | B 폭탄·G 황금·1칸 블록을 모두 10줄 보상에 연결. 각 기능·화면·연결 테스트 | #23, #28 |
| R2-12 | 문자 구분·색각 설정·쉬운 확장·명세 | L/W/B/G/1과 공통 PieceRules·모양 정보, [아이템 명세](game-guide.md#아이템-등장) | #21, #23, #28 |
| R2-13 | 작업 배정·진척·브랜치·PR·이슈 추적 | [대표 이슈 #19](https://github.com/dark-ran/tetris-team-project/issues/19), PR 작성·병합 이력, [작업 현황](req2-team-plan.md) | #19 |
| R2-14 | 중간 줄 커버리지 50% | UI·Main 포함 전체 2,229/2,384줄 = 93.50%. 3 OS CI에서 자동 검사 | #20, #26 |

PR 번호는 [저장소 Pull requests](https://github.com/dark-ran/tetris-team-project/pulls?q=is%3Apr+is%3Aclosed)에서 확인한다. 요구사항의 구현 상태와 실제 OS 확인 범위는 분리하여 기록한다.
