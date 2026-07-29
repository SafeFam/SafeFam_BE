FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S safefam \
    && adduser -S safefam -G safefam

COPY --from=builder \
    /workspace/build/libs/*.jar \
    app.jar

USER safefam

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]