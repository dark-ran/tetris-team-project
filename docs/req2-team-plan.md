# 작업 현황과 담당 이력

2026-10-27 기준. 기본 게임과 아이템 확장을 main에 통합했다. 완료 작업은 병합 PR로 추적하고 운영체제별 실제 실행 결과는 [대표 이슈 #19](https://github.com/dark-ran/tetris-team-project/issues/19)에서 모은다.

## 통합된 작업

아래 작성자는 GitHub PR의 작성 계정이다. 후속 보완은 해당 PR의 커밋·검토 이력에서 확인한다.

| 작업 | 작성 계정 | PR | 상태 |
|---|---|---|---|
| 보드·블록·게임 엔진 | todayoneul | #6 | main 반영 |
| 설정·파일 저장 | zinlinnhtoo | #7 | main 반영 |
| Swing 화면 | Tmei-ume1215 | #8 | main 반영 |
| 앱 제어·점수·속도·랭킹 | dark-ran | #11 | main 반영 |
| CI와 개발 환경 | dark-ran | #13 | main 반영 |
| 화면·입력·설정 통합 | todayoneul | #14 | main 반영 |
| Windows 배포 | dark-ran | #15 | main 반영 |
| macOS 배포·커버리지 기준 | todayoneul | #20 | main 반영 |
| 아이템 공통 구조·L·W | todayoneul | #21 | main 반영 |
| 난이도·가중치·속도·1칸 모양 | dark-ran | #22 | main 반영 |
| 폭탄·황금 블록 | todayoneul | #23 | main 반영 |
| 줄 삭제 반짝임과 연출 정리 | Tmei-ume1215 | #24 | 보완 후 main 반영 |
| 연출 중 진행 보류·회귀 검사 | dark-ran | #25 | main 반영 |
| 생성 확률·Req2 검사 | zinlinnhtoo | #26 | 보완 후 main 반영 |
| 앱 제어 주석 정리 | dark-ran | #27 | main 반영 |
| 난이도 기록·1칸 보상 연결 | todayoneul | #28 | main 반영 |
| 동일 기록 중 마지막 행 강조 | todayoneul | #29 | main 반영 |

[PR 목록](https://github.com/dark-ran/tetris-team-project/pulls?q=is%3Apr+is%3Aclosed) · [요구사항별 대응](requirements.md) · [검증 결과](verification.md)

## 확정된 규칙

- 보상 아이템은 L·W·B·G·1의 5종이며 각각 20%.
- 1칸 블록에도 기존 자동 낙하를 적용한다.
- 모래·중력 붕괴·변신 블록은 현재 배포 범위에 포함하지 않는다.
- 난이도는 다음 게임부터 적용하고 기록은 실제 플레이 난이도를 저장한다.
- 모드별 Top 10 안에 난이도를 표시한다.
- W·B는 점수 0, G는 해당 줄 삭제 처리 점수만 5배.
- 공통 구조와 효과 순서는 [구조 설명](architecture.md), 상세 동작은 [게임 안내](game-guide.md)로 관리한다.

## 평가 전 현장 확인

| 확인할 내용 | 상태 | 담당 처리 |
|---|---|---|
| macOS arm64 앱 실행·조작·저장 | 확인 완료 | 결과를 검증 문서에 기록 |
| Windows x64 실행·조작·저장 | CI·패키지 완료, 실제 장비 확인 필요 | Windows 사용 팀원이 실행 후 확인자·환경 기록 |
| 평가용 파일 사전 다운로드·다른 PC 첫 실행 | 평가 장비 확인 필요 | 평가에 사용할 PC 담당자가 확인 |

실제 확인 전 담당 계정이나 완료 상태를 임의로 지정하지 않는다. 열린 작업의 담당자와 기한은 대표 이슈에서 팀이 확정한다. 기존 이슈 #16·#17·#18은 #19로 통합하였다.
