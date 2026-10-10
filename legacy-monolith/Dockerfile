# Build context: repository root (needs the parent POM).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY legacy-monolith/pom.xml legacy-monolith/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl legacy-monolith -am dependency:go-offline
COPY legacy-monolith/src legacy-monolith/src
# Tests run via ./mvnw-docker; the image build must not depend on a Docker daemon.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl legacy-monolith -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/legacy-monolith/target/legacy-monolith-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
