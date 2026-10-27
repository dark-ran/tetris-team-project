# macOS 배포와 실행 확인

현재 패키지 다운로드와 확인 결과는 [배포 안내](distribution.md), [통합 검증](verification.md)에 정리한다. macOS에서는 아이콘이 포함된 Tetris.app을 운영체제로 열어 실행한다.

## 첫 실행이 차단될 때

현재 앱은 Apple 개발자 ID 인증서로 서명하거나 Apple 공증을 받지 않았다. GitHub에서 내려받은 앱을 처음 열면 **‘Tetris’을(를) 열지 않음** 또는 **악성 코드가 없음을 확인할 수 없음** 경고와 함께 macOS가 실행을 차단할 수 있다.

저장소의 배포 링크에서 받은 앱인지 확인한 뒤 다음 순서로 진행한다.

1. 경고 창에서 **완료**를 누른다.
2. **시스템 설정 → 개인정보 보호 및 보안**을 열고 아래쪽 **보안** 영역으로 이동한다.
3. Tetris에 대한 **그래도 열기**를 누른다.
4. 인증을 완료하고 다시 나타나는 창에서 **열기**를 선택한다.

허용한 앱은 이후 더블클릭으로 실행할 수 있다. 버튼이 보이지 않으면 앱을 다시 열어 경고를 표시한 뒤 설정을 확인한다. [Apple 공식 안내](https://support.apple.com/ko-kr/102445)

CI는 빌드·테스트·패키지 생성을 검사한다. Apple 공증과 다운로드한 앱의 첫 실행 허용은 별도로 확인해야 한다. 같은 방식으로 다시 빌드해도 이 경고는 발생할 수 있으며, 이 차단 경고를 없애려면 배포 과정에 Apple 개발자 인증 서명과 공증을 추가해야 한다.

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

평가 PC에서 위의 첫 실행 허용 절차를 미리 확인한다. 이 절차까지 금지되는 평가 방식이면 개발자 인증 서명·공증 배포를 준비한다.

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
