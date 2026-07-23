# Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -pl payment-service -am -DskipTests

# Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/payment-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]