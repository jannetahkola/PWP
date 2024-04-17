FROM maven:3.9 AS build

WORKDIR /app
COPY --from=build-core-with-deps /root/.m2 /root/.m2
COPY --from=build-core-with-deps /app/ /app
COPY palikka-users-api/src /app/palikka-users-api/src

RUN mvn -B -e clean install -DskipTests=true -pl '.,palikka-users-api'

FROM eclipse-temurin:21

WORKDIR /app
COPY --from=build /app/palikka-users-api/target/palikka-users-api-0.0.1-SNAPSHOT.jar .

CMD ["java", "-jar", "palikka-users-api-0.0.1-SNAPSHOT.jar"]