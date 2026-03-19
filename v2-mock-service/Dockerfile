FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY v2-mock-service/pom.xml .
COPY v2-mock-service/src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/v2-mock-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
