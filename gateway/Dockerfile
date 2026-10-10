FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY legacy-monolith/pom.xml legacy-monolith/
COPY gateway/pom.xml gateway/
COPY web/pom.xml web/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl gateway -am dependency:go-offline
COPY gateway/src gateway/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/gateway/target/gateway-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
