# ---- Build Stage ----
FROM gradle:8.7-jdk17 AS build

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN gradle build --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*-all.jar app-all.jar

ENTRYPOINT ["tail", "-f", "/dev/null"]
