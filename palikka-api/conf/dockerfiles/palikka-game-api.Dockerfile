FROM maven:3.9 AS build

ENV DOCKER_HOST=tcp://host.docker.internal:2375
ENV TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal

WORKDIR /app
COPY --from=build-core-with-deps /root/.m2 /root/.m2
COPY --from=build-core-with-deps /app/ /app
COPY ../../palikka-game-api/src /app/palikka-game-api/src

WORKDIR /app/palikka-game-api
COPY ../../sonar.txt /app/palikka-game-api/sonar.txt
COPY ../../lombok.config /app/palikka-game-api/lombok.config
RUN mvn clean verify \
    sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login="$(cat sonar.txt)"

FROM eclipse-temurin:21

WORKDIR /app
COPY --from=build /app/palikka-game-api/target/palikka-game-api-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "palikka-game-api-0.0.1-SNAPSHOT.jar"]