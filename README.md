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
