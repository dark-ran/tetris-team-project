# 배포와 실행

배포 패키지는 Java 실행 환경과 아이콘을 포함한다. 사용자는 소스 빌드나 Java 실행 명령 없이 압축을 풀고 실행파일을 연다.

## 중간평가용 패키지

기능 기준: main `72207af` (2026-10-27, PR #31의 게임 종료 시 스코어보드 표시 수정 포함). 이후 문서만 바뀐 경우 아래 패키지의 게임 기능은 같다.

| 운영체제·CPU | 다운로드 | 실행 위치 |
|---|---|---|
| Windows x64 | [Tetris-windows-x64](https://github.com/dark-ran/tetris-team-project/actions/runs/40417106166/artifacts/12695887927) | 내부 ZIP 해제 후 `Tetris/Tetris.exe` |
| macOS Apple Silicon arm64 | [Tetris-macos-ARM64](https://github.com/dark-ran/tetris-team-project/actions/runs/40417106164/artifacts/12694424200) | 내부 ZIP 해제 후 `Tetris.app` |

GitHub에 로그인하여 Actions Artifact를 받는다. 바깥 Artifact ZIP 안에는 게임 ZIP과 SHA-256 확인값 파일이 있다. 내부 게임 ZIP도 해제한 뒤 실행한다. Windows에서는 exe만 따로 옮기지 말고 Tetris 폴더 전체를 유지한다.

Artifact는 보관 기간이 있으므로 평가 전에 내려받아 둔다. 최신 실행은 [Package Windows](https://github.com/dark-ran/tetris-team-project/actions/workflows/package-windows.yml)와 [Package macOS](https://github.com/dark-ran/tetris-team-project/actions/workflows/package-macos.yml)에서 확인한다. CI 실행의 브랜치·커밋·성공 여부를 함께 확인한다.

macOS arm64는 로컬 실행·조작·설정과 점수 재로딩을 확인했다. Windows는 자동 검사와 패키지 생성을 확인했으며 실제 장비의 조작 확인은 별도로 필요하다. Intel Mac 패키지는 Intel Mac/JDK에서 생성·검증해야 하며 위 arm64 파일을 Intel용으로 표시하지 않는다.

## 패키지 다시 만들기

소스 빌드에는 JDK 21을 사용한다. 패키지는 대상 OS에서 만든다.

```bash
# macOS
./gradlew --no-daemon clean build jacocoTestReport packageMacos
```

```powershell
# Windows x64
.\gradlew.bat --no-daemon clean build jacocoTestReport packageWindows
```

| 산출물 | 위치 |
|---|---|
| macOS 앱 | `build/macos/Tetris.app` |
| macOS 전달 파일 | `build/macos/Tetris-macos-arm64.zip` 또는 `Tetris-macos-x86_64.zip` |
| Windows 실행파일 | `build/windows/Tetris/Tetris.exe` |
| Windows 전달 파일 | `build/windows/Tetris-windows-x64.zip` |
| 확인값 | 각 ZIP 옆의 `.sha256` |

`gradlew`·`gradlew.bat`와 Wrapper JAR는 저장소에 포함한다. `build/`는 생성물이며 Git에서 제외한다. `clean`은 생성물을 지우므로 이후 패키지가 필요하면 다시 만들거나 Artifact를 받는다.

로컬 macOS arm64 산출물은 앱 약 78MiB, ZIP 약 24MiB다. 설치 폴더와 별도로 사용자 홈에 설정·점수를 저장한다. 앱 교체 시 해당 파일을 지울 필요가 없다.

## 평가 장비에서 확인

1. OS·CPU에 맞는 파일을 받고 압축을 해제한다.
2. 아이콘이 있는 앱 또는 exe를 더블클릭하여 시작 메뉴를 연다.
3. 이동·회전·즉시 낙하, 게임 메뉴·재개, 일반·아이템 모드 전환을 확인한다.
4. 설정과 점수를 저장한 뒤 종료·재실행하여 유지 여부를 확인한다.
5. 확인자·OS/CPU·기능 커밋·결과를 [대표 이슈 #19](https://github.com/dark-ran/tetris-team-project/issues/19)에 기록한다.

macOS 패키지는 개발자 서명·공증을 제공하지 않는다. 인터넷에서 받은 앱의 첫 실행이 차단되면 시스템 설정의 개인정보 보호 및 보안에서 실행 허용이 필요할 수 있다. 이 첫 실행 절차까지 포함하여 평가 장비에서 사전에 확인한다. [macOS 세부 안내](macos-distribution.md)
