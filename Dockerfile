# Linux 빌드·단위 테스트 전용. GUI 및 OS별 배포 환경이 아니다.
# Docker Official Images의 태그와 레지스트리 manifest digest를 확인하여 고정했다.
FROM eclipse-temurin:21.0.12_8-jdk-jammy@sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle gradle.properties gradle.lockfile ./
COPY gradle/ gradle/
COPY src/ src/

RUN chmod +x gradlew && ./gradlew --no-daemon compileJava test

CMD ["./gradlew", "--no-daemon", "clean", "compileJava", "test"]
