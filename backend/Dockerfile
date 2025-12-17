# Multi-stage Dockerfile: build with Maven, run on a slim Eclipse Temurin JRE 21 image
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

# only copy pom first to leverage Docker layer cache for dependencies
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests by default for faster builds; CI can run tests separately)
RUN mvn -B -DskipTests package

## Runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the jar built in the previous stage. Using pattern to match final artifact name.
COPY --from=build /workspace/target/*-SNAPSHOT.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
