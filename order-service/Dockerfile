# Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml ./
COPY common-libs/pom.xml common-libs/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY delivery-service/pom.xml delivery-service/pom.xml

RUN mvn dependency:go-offline -pl common-libs,order-service -am -B -q

COPY common-libs/src common-libs/src
COPY order-service/src order-service/src

RUN mvn package -pl common-libs,order-service -am -DskipTests -B -q

# Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/order-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
