# macOS 배포와 실행 확인

Req1 18쪽은 아이콘이 있고 운영체제에서 더블클릭해 실행하는 패키지를 요구한다.
Windows 또는 macOS 중 하나만 대상으로 해도 된다. macOS에서는 `.app`이 실행파일에 해당한다.

## 만드는 방법

macOS와 JDK 21에서 `./gradlew packageMacos`를 실행한다.
Gradle이 게임 JAR와 실행 의존성을 모으고, 아이콘을 그린 뒤 JDK의 `jpackage`로 앱을 만든다.
Java 실행 환경도 포함한다. 아이콘 생성에는 Java와 macOS 기본 도구만 사용한다.
앱 조립은 임시 폴더에서 수행하므로 iCloud 폴더에 붙는 Finder 정보가 서명을 방해하지 않는다.
`jdk.random`도 포함하므로 게임에서 사용하는 기본 난수 생성기를 사용할 수 있다.

- 로컬 실행: `build/macos/Tetris.app`
- 전달 파일: `build/macos/Tetris-macos-arm64.zip` 또는 `Tetris-macos-x86_64.zip`
- 파일 확인값: ZIP 옆의 `.sha256`
- 다시 만들면 `build/macos`의 이전 생성물을 교체한다.

Mac의 CPU와 선택된 JDK의 CPU가 같아야 한다. 기본 작업은 빌드한 CPU 한 종류만 지원한다.
두 CPU를 지원하려면 각각의 Mac/CI에서 해당 CPU의 JDK로 만들고 검증한다.
생성된 실행파일과 동봉 Java는 `build/`에 두고 Git에는 추가하지 않는다.
배포 파일은 Actions Artifact 또는 팀이 정한 릴리스 파일로 전달한다.

## 평가 전 직접 확인할 순서

1. 평가에 사용할 macOS 26 이상 PC에서 ZIP을 내려받아 Finder로 압축을 푼다.
2. 앱을 더블클릭해 시작 메뉴가 나오는지 확인한다. 별도 Java 설치나 명령어를 요구하면 안 된다.
3. 게임 시작, 이동·회전·즉시 낙하, 일시정지·재개, 설정 변경, 메뉴 복귀, 종료를 확인한다.
4. 다시 실행해 설정과 점수가 유지되는지 확인한다.
5. 테스트한 OS·CPU·소스 커밋·파일 확인값과 결과를 기록한다.

현재 작업은 개발자 서명과 Apple 공증을 제공하지 않는다.
인터넷에서 받은 앱은 Gatekeeper가 차단할 수 있으며, 그 경우 macOS의
시스템 설정 → 개인정보 보호 및 보안에서 해당 앱의 실행을 허용해야 할 수 있다.
따라서 다른 PC에서 아무 추가 설정 없이 실행되는지까지는 로컬 빌드 성공만으로 확인할 수 없다.
평가 PC에서 첫 실행을 미리 확인하고, 이 절차까지 금지되는 평가 방식이면 서명·공증 배포를 준비한다.

## 2026-10-22 로컬 확인 결과

macOS 26.6.2 / Apple Silicon arm64 / Temurin JDK 21.0.12.1에서 확인했다.
패키지에는 Java 실행 환경과 `java.desktop`, `jdk.random` 및 필요한 모듈이 포함된다.
앱의 크기는 약 77 MiB, ZIP은 약 24 MiB다.
로컬 앱을 운영체제로 실행해 시작 메뉴, 게임 시작, 좌우 이동·회전·즉시 낙하,
Esc 게임 메뉴와 프로그램 종료를 확인했다. 실제 창 자동 테스트 9개도 통과했다.
이 결과는 인터넷에서 내려받은 다른 Mac의 최초 실행을 검증한 것은 아니다.

참고: [Apple의 확인되지 않은 개발자 앱 실행 안내](https://support.apple.com/guide/mac-help/open-a-mac-app-from-an-unknown-developer-mh40616/mac),
[JDK 21 jpackage 공식 설명](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html).
OS별 패키지는 해당 OS에서 만들어야 한다.
