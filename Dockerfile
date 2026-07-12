# 1. 빌드 스테이지: Gradle을 이용해 JAR 파일 생성
FROM gradle:8.5-jdk17 AS builder
WORKDIR /workspace

# 빌드 속도 최적화를 위해 의존성 파일 먼저 캐싱
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드 (테스트는 인프라 파이프라인이나 로컬에서 검증하므로 제외)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 2. 실행 스테이지: 경량화된 JRE 환경에서 JAR 실행
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 가져오기
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar

# Spring Boot 기본 포트 개방
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
