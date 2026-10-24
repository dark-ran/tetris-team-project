# tetris-team-project
SeoulTech Software Engineering Team Project - Tetris

## 실행 및 로컬 검증

JDK 21 환경에서 Gradle Wrapper를 사용한다.

```bash
./gradlew run
./gradlew test jacocoTestReport
```

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용한다.

## Windows 실행파일

Windows x64와 JDK 21에서 다음 명령을 실행하면 아이콘, `Tetris.exe` 및
동봉 Java 실행 환경이 포함된 패키지를 자동 생성한다.

```powershell
.\gradlew.bat clean packageWindows
```

생성된 `build/windows/Tetris/Tetris.exe`를 더블클릭해 실행한다.
배포할 때는 `build/windows/Tetris-windows-x64.zip`을 사용한다.
받는 PC에는 별도의 Java 설치가 필요하지 않다.

`clean`은 `build/`의 산출물을 삭제하므로, 실행파일이 필요한 경우
`packageWindows`를 다시 실행하거나 GitHub의 `Package Windows` Actions
Artifact에서 ZIP을 내려받는다. `build/`는 Git에 추가하지 않는다.
Windows x64에서 패키지를 생성하며 다른 CPU 아키텍처는 지원하지 않는다.

## macOS 실행파일

macOS에서 JDK 21로 다음 명령을 실행하면 아이콘과 Java 실행 환경이 포함된 앱을 만든다.

```bash
./gradlew packageMacos
```

생성된 `build/macos/Tetris.app`을 Finder에서 더블클릭하면 실행된다.
다른 사람에게 전달할 때는 같은 폴더의 `Tetris-macos-arm64.zip` 또는
`Tetris-macos-x86_64.zip`을 전달한다. 받는 사람은 ZIP을 풀고 앱을 더블클릭한다.
실행하는 사람의 Java 설치나 터미널 명령은 필요하지 않다.

빌드한 Mac/JDK의 CPU에 맞는 패키지가 생성된다. Apple Silicon과 Intel은 각각 빌드·검증한다.
현재 패키지는 개발자 서명·공증을 하지 않은 수업용 앱이므로, 인터넷으로 받은 파일은
macOS에서 차단될 수 있다. 다른 Mac에서의 첫 실행과 허용 절차는
[배포 점검표](docs/macos-distribution.md)를 확인한다.

GitHub의 `Package macOS` Actions에서도 실행 패키지를 다운로드할 수 있다.
이 패키지는 현재 소스의 기능을 담으며, 아이템 모드 구현 완료를 의미하지 않는다.

## 상호평가 준비

[요구사항·설계·검증 점검](docs/peer-review-readiness.md)에 현재 상태와 작업 순서를 정리했다.
[팀원 의견별 진행안](docs/req2-team-plan.md)에서는 필수 요구사항·아이템 후보·역할 분담·OS별 검증을 구분한다.
`./gradlew check`는 UI와 `Main`을 포함한 전체 코드의 줄 커버리지 50% 이상도 검사한다.
기존 보드·블록·엔진의 클래스별 70% 검사는 함께 유지한다.

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
