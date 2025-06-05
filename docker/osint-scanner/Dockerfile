FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies
COPY src ./src
RUN gradle clean shadowJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./osint-scanner.jar
COPY src/main/resources ./resources
ENTRYPOINT ["java", "-jar", "osint-scanner.jar"]
