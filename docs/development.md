# 개발과 검증

Java 21, Gradle Wrapper 8.14.4, Swing, JUnit 5, JaCoCo를 사용한다. Gradle과 Wrapper JAR는 저장소에 포함되어 별도 Gradle 설치 없이 빌드할 수 있다.

## 명령

```bash
./gradlew run
./gradlew --no-daemon clean build jacocoTestReport
./gradlew reviewWindowTest
./gradlew gameplayPerformanceTest
./gradlew packageMacos
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용한다. Windows 패키지는 `.\gradlew.bat packageWindows`로 생성한다.

`build`는 단위 검사, 화면 없는 통합 검사, 미사용 import, 전체 줄 50%, 핵심 클래스별 70% 기준을 실행한다. 실제 창 검사는 GUI 환경에서, 성능 검사는 256MiB 힙으로 별도 실행한다. 머신별 시간이 달라지는 성능 검사는 일반 CI에 강제하지 않는다.

| 보고서 | 위치 |
|---|---|
| 단위 | `build/reports/tests/test/index.html` |
| 통합 | `build/reports/tests/reviewIntegrationTest/index.html` |
| 실제 창 | `build/reports/tests/reviewWindowTest/index.html` |
| 성능 | `build/reports/tests/gameplayPerformanceTest/index.html` |
| 커버리지 | `build/reports/jacoco/test/html/index.html` |
| 성능 측정값 | `build/reports/gameplay-performance/` |

## 협업 흐름

기능별 브랜치에서 구현·명세·검사를 함께 변경한다. PR에는 해결하는 문제, 동작 규칙과 검증 결과를 기록한다. 최신 main과의 충돌을 정리하고 세 OS CI를 확인한 뒤 통합한다. 게임 코드 변경은 Windows·macOS 패키지 생성도 확인한다.

CI는 push와 pull_request에 각각 실행된다. 같은 커밋에 두 이벤트가 연결되면 검사 수가 늘어나고, main 병합 커밋에는 push 검사만 연결된다. CI 개수보다 해당 커밋의 검사 이름·결과·패키지 성공을 확인한다.

담당 이력은 [작업 현황](req2-team-plan.md), 현재 결과는 [검증 문서](verification.md)에 정리한다. `build/`, 로컬 IDE 설정, 사용자 점수 파일은 커밋하지 않는다.

## 아이템 설명 이미지 재생성

`docs/tools/GenerateItemGallery.java`는 실제 Tetromino와 PieceStyle을 사용한다. 빌드한 main 클래스에 연결해 PNG 두 개를 만든다.

```bash
./gradlew classes
mkdir -p build/docs-tools
javac -encoding UTF-8 -cp build/classes/java/main -d build/docs-tools docs/tools/GenerateItemGallery.java
java -Djava.awt.headless=true -cp build/docs-tools:build/classes/java/main tetris.ui.GenerateItemGallery docs/images
```

위 이미지 명령의 클래스 경로 구분자는 macOS/Linux의 `:` 기준이다. Windows에서는 `;`로 바꾼다. 앱 스크린샷은 실제 배포 앱을 실행해 촬영하며 설명용 기록과 검증 환경을 함께 적는다.
