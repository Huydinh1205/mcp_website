# --- build ---------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src/ src/
RUN mvn -q -B -Dmaven.test.skip=true package

# --- run ----------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/negotiation-backend-0.1.0.jar app.jar
# Render/Railway/Fly inject $PORT; application.yml reads it (default 8080).
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
