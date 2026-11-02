# Stage 1: Build
FROM gradle:9.3.0-jdk25-alpine AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
COPY config ./config
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
