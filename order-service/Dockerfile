# Build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml ./

COPY common-libs/pom.xml common-libs/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml


COPY order-service/pom.xml order-service/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

RUN mvn dependency:go-offline -pl common-libs,order-service -am -B -q

COPY common-libs/src common-libs/src
COPY order-service/src order-service/src

RUN mvn clean package -pl common-libs,order-service -am -B -q

# Run
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder \
     --chown=spring:spring \
     /build/order-service/target/*.jar app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
