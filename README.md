# tetris-team-project
SeoulTech Software Engineering Team Project - Tetris

## 실행 및 로컬 검증

JDK 21 환경에서 Gradle Wrapper를 사용한다.

```bash
./gradlew run
./gradlew test jacocoTestReport
```

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용한다.

## GitHub Actions

[Java CI](.github/workflows/ci.yml)는 모든 push와 pull request에 자동으로 실행된다.
Temurin Java 21과 저장소의 Gradle Wrapper를 사용해 Ubuntu·Windows·macOS에서
`clean build jacocoTestReport`를 수행하므로, PR을 병합하기 전에 세 운영체제의 결과를 확인한다.

## 보드와 블록 구현

`Board`, `PieceType`, `Tetromino`, `PieceQueue`의 호출 방법과 좌표·칸 저장값 규칙은
[보드와 블록의 연결 규칙](docs/contracts/board-and-piece.md)에 기록한다.
완료 기준, 요구사항별 테스트, 검증 결과와 후속 작업은
[보드와 블록 개발 기록](docs/work-items/board-and-piece.md)에서 확인한다.

해당 기능만 빠르게 검사하려면 다음 명령을 사용한다.

```bash
./gradlew test --tests 'tetris.board.*' --tests 'tetris.piece.*' boardAndPieceCoverageVerification
```

전체 변경 검증은 `./gradlew clean build jacocoTestReport`로 실행한다.
테스트 결과는 `build/reports/tests/test/index.html`, 코드 실행 범위는
`build/reports/jacoco/test/html/index.html`에서 확인한다.

## 게임 엔진 구현

게임 행동·상태·고정·줄 삭제·게임오버를 구현했다.
[게임 엔진 연결 규칙](docs/contracts/game-engine.md)과
[개발 기록 및 검증 결과](docs/work-items/game-engine.md)를 기준으로 다른 모듈을 연결한다.

엔진 기능만 검증하려면 다음 명령을 사용한다.

```bash
./gradlew test --tests 'tetris.game.*' gameEngineCoverageVerification
```

실제 게임 플레이에는 앱 제어·입력·게임 루프·화면 상태 표시 연결이 필요하다.

## 문서 안내

- [모듈 연결 안내](docs/contracts/README.md): 파트별 책임, 클래스·메서드의 사용 방법과 확장 예시.
- [작업 기록과 이슈 연결](docs/work-items/README.md): 기능 범위, 완료 기준, 검증 결과와 후속 작업.
