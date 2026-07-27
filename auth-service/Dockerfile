# Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

COPY auth-service/pom.xml auth-service/pom.xml

RUN mvn dependency:go-offline -f auth-service -B -q

COPY auth-service/src auth-service/src

RUN mvn clean package -f auth-service/pom.xml -DskipTests -B -q

# Run
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY --from=builder /build/auth-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]