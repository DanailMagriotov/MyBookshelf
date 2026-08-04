# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
    && cp target/message-service-*.jar /app/app.jar

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
