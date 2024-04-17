FROM maven:3.9 AS build

WORKDIR /app
COPY --from=build-core-with-deps /root/.m2 /root/.m2
COPY --from=build-core-with-deps /app/ /app
COPY palikka-game-api/src /app/palikka-game-api/src

RUN mvn -B -e clean install -DskipTests=true -pl '.,palikka-game-api'

FROM eclipse-temurin:21

WORKDIR /app
COPY --from=build /app/palikka-game-api/target/palikka-game-api-0.0.1-SNAPSHOT.jar .

CMD ["java", "-jar", "palikka-game-api-0.0.1-SNAPSHOT.jar"]