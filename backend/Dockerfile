# Multi-stage Dockerfile: build with Maven, run on a slim Eclipse Temurin JRE 25 image
FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace

# only copy pom first to leverage Docker layer cache for dependencies
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests by default for faster builds; CI can run tests separately)
RUN mvn -B -DskipTests package

## Runtime image
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# Install ffmpeg for Speaking grading audio transcoding.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg \
 && ffmpeg -version \
 && rm -rf /var/lib/apt/lists/*

# Copy the jar built in the previous stage. Using pattern to match final artifact name.
COPY --from=build /workspace/target/*-SNAPSHOT.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
