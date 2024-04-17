FROM maven:3.9 AS build

ENV DOCKER_HOST=tcp://host.docker.internal:2375
ENV TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal

WORKDIR /app
COPY ../../sonar.txt .
COPY --from=build-core-with-deps /root/.m2 /root/.m2
COPY --from=build-core-with-deps /app/ /app
COPY ../../palikka-users-api/src /app/palikka-users-api/src

WORKDIR /app/palikka-users-api
COPY ../../sonar.txt /app/palikka-users-api/sonar.txt
COPY ../../lombok.config /app/palikka-users-api/lombok.config
RUN mvn clean verify \
    sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login="$(cat sonar.txt)"

FROM eclipse-temurin:21

WORKDIR /app
COPY --from=build /app/palikka-users-api/target/palikka-users-api-0.0.1-SNAPSHOT.jar .

CMD ["java", "-jar", "palikka-users-api-0.0.1-SNAPSHOT.jar"]