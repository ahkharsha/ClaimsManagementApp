FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables (overridable at runtime)
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/insuranceiq_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=root
ENV APP_FRAUD_SERVICE_URL=http://ml-service:8000
ENV APP_NOTIFICATION_SERVICE_URL=http://notification-service:5001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
