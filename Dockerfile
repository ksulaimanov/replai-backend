FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
RUN chmod +x ./mvnw || true
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE ${PORT}
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]

