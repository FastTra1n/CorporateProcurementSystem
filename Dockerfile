# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
COPY cps-core/build.gradle.kts cps-core/build.gradle.kts
COPY cps-backend/build.gradle.kts cps-backend/build.gradle.kts
COPY cps-console/build.gradle.kts cps-console/build.gradle.kts
COPY cps-desktop/build.gradle.kts cps-desktop/build.gradle.kts
COPY cps-core/src/main/ cps-core/src/main/
COPY cps-backend/src/main/ cps-backend/src/main/

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew :cps-backend:bootJar --no-daemon --console=plain

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 cps \
    && useradd --uid 10001 --gid cps --no-create-home --home-dir /app --shell /usr/sbin/nologin cps \
    && mkdir -p /app/logs \
    && chown cps:cps /app /app/logs

COPY --from=build --chown=cps:cps /workspace/cps-backend/build/libs/cps-backend.jar /app/cps-backend.jar
USER cps
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/cps-backend.jar"]
