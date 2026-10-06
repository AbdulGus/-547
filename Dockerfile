FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean verify

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S library && adduser -S library -G library
COPY --from=build /build/target/library-1.0.0.jar app.jar
USER library
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
