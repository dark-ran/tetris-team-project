# Development Guide

> 이 문서는 개발 환경과 GitHub 협업 방식에 대한 **초안/제안안**입니다.
> 팀 합의에 따라 변경할 수 있습니다.

## 1. 기술 스택 제안

| 항목 | 제안 | 비고 |
|---|---|---|
| Language | Java 21+ | Java 21이상인 버젼에서 작동되어야 함 |
| GUI | Java Swing | Java 기본 제공 GUI 사용 제안 |
| Build Tool | Gradle | Build/Test/Coverage 자동화 |
| Gradle DSL | Groovy DSL (`build.gradle`) | 단순한 설정을 위해 제안 |
| Gradle 실행 | Gradle Wrapper | 팀원별 Gradle 별도 설치 불필요 |
| Unit Test | JUnit 5 | 단위 테스트 |
| Coverage | JaCoCo | Line Coverage 관리 |
| VCS | Git + GitHub | 코드 및 변경 이력 관리 |
| CI | GitHub Actions | Windows/macOS Push/PR 자동 검증 제안 |
| 배포 | jpackage | OS별 Windows 실행파일/macOS 앱 생성 제안 |
| 저장 | 파일 기반 | 설정/스코어보드 영구 저장 방식 제안 |
| 대상 OS | Windows 11 + macOS 26 이상 | 팀원 소유 기기별 배포 및 테스트 진행 |

### 사용하지 않는 방향

현재 제안에서는 다음을 사용하지 않는다.

```text
Maven / pom.xml / mvn
Kotlin / build.gradle.kts / kotlinc
```

이는 과제 금지사항이 아니라 팀 개발환경 단순화를 위한 제안이다.

## 2. Gradle Wrapper

Repository에 다음 파일을 포함한다.

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Gradle Wrapper를 사용하면 팀원이 Gradle을 별도로 설치하지 않아도 된다.

JUnit 5와 JaCoCo도 Gradle dependency/plugin 설정을 통해 내려받아 사용하도록 구성한다. macOS에서는 `./gradlew`의 실행 권한도 확인한다.

## 3. 기본 명령

### Windows Build + Test

```powershell
.\gradlew.bat clean build
```

### 실행

```powershell
.\gradlew.bat run
```

### Coverage Report

```powershell
.\gradlew.bat test jacocoTestReport
```

### macOS Build / Run / Coverage

```bash
./gradlew clean build
./gradlew run
./gradlew test jacocoTestReport
```

위 명령은 개발자용이며, 최종 사용자에게는 별도 명령 없이 실행되는 배포 패키지를 제공한다.

## 4. build.gradle 기본 구성 예시

```groovy
plugins {
    id 'java'
    id 'application'
    id 'jacoco'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation platform(
        'org.junit:junit-bom:5.11.0'
    )

    testImplementation(
        'org.junit.jupiter:junit-jupiter'
    )
}

application {
    mainClass = 'tetris.Main'
}

test {
    useJUnitPlatform()
}
```

실제 dependency 버전과 packaging 설정은 구현 시 확정한다. `jacocoTestReport` 생성과 `jacocoTestCoverageVerification`의 기준값을 설정하고, `check`에 검증 작업을 연결하여 CI에서 기준 미달을 감지하도록 구성한다.

## 5. Git Branch 전략 제안

사람 이름보다 기능 단위 Branch를 사용한다.

```text
main
develop

feature/board
feature/collision
feature/rotation
feature/game-engine
feature/scoring
feature/start-menu
feature/settings-ui
feature/scoreboard
feature/settings-storage
```

Issue와 연결하는 경우:

```text
feature/12-collision
feature/17-scoreboard
```

`develop` Branch 사용 여부는 팀 합의 후 결정한다.
팀 규모가 작고 관리 복잡도가 커진다면 `main + feature branch` 방식으로 단순화할 수도 있다.

## 6. Pull Request 흐름 제안

`develop`을 사용하는 경우:

```text
feature branch
      ↓
Pull Request
      ↓
다른 팀원 Review
      ↓
develop
      ↓
전체 Build / Test
      ↓
main
```

`develop`을 사용하지 않는 경우:

```text
feature branch
      ↓
Pull Request
      ↓
다른 팀원 Review
      ↓
Build / Test
      ↓
main
```

PR 병합 전에는 관련 Issue 연결, 완료 기준 충족, 관련 테스트·CI 통과, 동료 리뷰 및 필요 문서 갱신을 확인한다.

## 7. Issue 사용

Issue는 다음과 같은 내용을 기록하는 데 사용한다.

- 구현해야 할 기능
- 버그
- 요구사항에서 모호한 부분
- 팀에서 결정해야 하는 사항
- 문제 발생 및 해결 과정
- 요구사항 ID, 담당자, 검증 가능한 완료 기준(Acceptance Criteria)

예:

```text
Issue: 낙하 속도 증가 기준 결정

문제:
PDF에는 일정 수 이상의 블럭 생성 또는 줄 삭제 후
속도를 증가시키라고 되어 있으나 구체적인 수치는 없음.

논의:
- 블럭 몇 개를 기준으로 할지
- 몇 줄 삭제를 기준으로 할지
- 최소 낙하 간격을 둘지

결정:
팀 논의 후 기록
```

PR 설명에서 관련 Issue를 연결할 수 있다.

```text
Closes #12
```

## 8. 문서 관리

`docs/` 아래에서 프로젝트 관련 문서를 관리한다.

```text
docs/
├── requirements.md
├── architecture.md
├── development.md
└── decisions/
```

- `requirements.md`: PDF 요구사항 정리 및 팀 결정 필요 사항
- `architecture.md`: 프로젝트 구조 및 설계 제안
- `development.md`: 개발환경, Git, Build/Test/CI 방식
- `decisions/`: 중요한 설계 결정이 별도 문서가 필요할 경우 사용

문서 수정도 가능하면 Branch → Commit → PR → Review 흐름을 사용한다. 요구사항 변경 시 관련 Issue·테스트를 함께 갱신하고, 중요한 설계 결정만 이유·대안과 함께 `decisions/`에 기록한다.

## 9. GitHub Actions 제안

Push 또는 PR 시 자동으로 다음을 수행하도록 구성한다.

```text
Checkout
↓
Java 21 설치
↓
Gradle Wrapper
↓
Build
↓
JUnit Test
↓
JaCoCo Coverage
```

예:

```yaml
- name: Build
  run: ./gradlew clean build
```

Coverage Report 생성 여부는 workflow와 `build.gradle`에서 명시적으로 설정한다.

- CI는 Windows 및 macOS 러너에서 Java 21 빌드·테스트를 수행하도록 제안한다. OS별 실행파일 패키징은 해당 OS 러너에서 별도 수행한다.
- CI 성공만으로 Windows 11/macOS 26 실기기 실행 또는 과제 최소 사양 충족을 입증했다고 보지 않고 별도로 검증한다.
- 위 예시 워크플로는 제안이며 실제 워크플로 파일을 생성하고 실행 결과를 확인해야 한다.

## 10. Windows 및 macOS 배포 제안

```text
Gradle
↓
JAR
↓
jpackage
↓
Windows 실행파일
```

목표:

- 아이콘이 포함된 실행파일 제공
- 사용자가 명령 프롬프트에서 Java 명령을 입력하지 않고 실행
- 더블클릭 등 일반적인 Windows 동작으로 실행 가능

실행파일 이름은 팀에서 결정한다.

macOS에서는 같은 Java 빌드 결과를 기반으로 macOS 환경에서 `jpackage`를 실행해 아이콘(`.icns`)이 포함된 `.app`을 생성하고 더블클릭 실행을 확인한다. 필요하면 `.dmg` 배포도 검토한다. Windows용 패키지는 Windows에서, macOS용 패키지는 macOS에서 각각 빌드한다.

최초 실행 시 macOS 보안 정책(서명·공증 여부)에 따른 동작과 데이터 저장·재실행을 확인하며, 과제 제출용 배포 범위는 팀에서 결정한다.

## 11. 팀 작업 원칙 제안

- 각자 담당 기능의 구현과 테스트를 함께 작성한다.
- 다른 담당자의 주요 영역을 수정해야 하면 먼저 공유한다.
- 큰 설계 변경이나 요구사항 해석 문제는 Issue 또는 PR에서 논의한다.
- 사소한 변경까지 모든 것을 Issue로 만들 필요는 없다.
- 중요한 결정과 해결 과정은 나중에 제시할 수 있도록 기록을 남긴다.
- 짧은 반복 주기마다 우선순위가 높은 작은 기능을 구현·테스트·통합하고 실행 가능한 결과물을 남긴다.
- 각 반복 주기에서 진행 상황·막힌 점을 공유하고, 완료 후 개선점을 다음 작업에 반영한다.
- 완료 정의(Definition of Done): 완료 기준 충족, 관련 테스트·CI 통과, 리뷰·통합 완료, 필요한 문서 갱신.
- 2·3차 요구사항이 공개되면 기존 기능 영향과 회귀 테스트를 검토한 뒤 Issue로 작업을 분리한다.

## 12. 현재 팀 논의가 필요한 항목

- 실제 역할 분담
- `develop` Branch 사용 여부
- 낙하 속도 증가 규칙
- 추가 점수 규칙
- Level 개념 도입 여부
- 색맹 모드의 구체적인 디자인
- 저장 파일 형식
- 패키지 구조 및 클래스 분리 수준
- 텍스트 기반 UI에서 Swing 허용 범위 및 표현 방식
- macOS CPU 아키텍처, OS별 저장 경로, 실행파일 검증 범위
- 반복 개발 주기·Issue 우선순위·완료 정의의 운영 방식
