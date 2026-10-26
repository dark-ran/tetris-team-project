# 작업 기록

현재 구현·병합 상태는 [작업 현황과 담당 이력](../req2-team-plan.md), 최종 검사 수와 배포 상태는 [통합 검증](../verification.md)에 정리한다.

이 폴더의 문서는 각 작업을 수행한 시점의 개발 기록이다. 문서에 적힌 ‘검토 대기’, ‘미푸시’, 당시 테스트 수는 작성 시점의 상태이며 현재 main의 상태를 나타내지 않는다.

| 기록 | 내용 |
|---|---|
| [보드와 블록](board-and-piece.md) | 좌표·회전·고정·큐와 초기 검증 |
| [게임 엔진](game-engine.md) | 행동·고정·줄 삭제·게임오버와 모듈 연결 |
| [색각·창 크기](display-settings.md) | 색각 모드·독립 패턴·크기 유지와 취소 |
| [이름 입력](game-over-registration.md) | 게임 종료 후 이름 입력 화면 |
| [점수 초기화](scoreboard-reset.md) | 저장·화면·실패 처리 |
| [속도](gameplay-speed.md) | 생성 수·삭제 수와 속도·점수 |
| [미리보기](next-piece-preview.md) | 보드 옆 배치와 표시 검사 |
| [코드 정리](source-hygiene.md) | import·성능·통합 검사 |
| [UI 통합](pr14-ui-sync.md) | 초기 통합과 충돌 정리 |
| [Req2 검사](req2-verification-report.md) | 난이도·확률 검사 추가 시점 기록 |

사용 방법과 현재 규칙은 [게임 안내](../game-guide.md), [모듈 연결 안내](../contracts/README.md)에서 확인한다.
