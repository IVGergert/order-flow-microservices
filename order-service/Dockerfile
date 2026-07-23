# Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -pl order-service -am -DskipTests

# Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/order-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]