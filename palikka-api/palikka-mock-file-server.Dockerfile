FROM maven:3.9 as build

WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /app/ /app
COPY palikka-mock-file-server/src /app/palikka-mock-file-server/src

RUN mvn -B -e clean install -DskipTests=true -pl '.,palikka-mock-file-server'

FROM eclipse-temurin:21

WORKDIR /app
COPY --from=build /app/palikka-mock-file-server/target/palikka-mock-file-server-0.0.1-SNAPSHOT.jar .

CMD ["java", "-jar", "palikka-mock-file-server-0.0.1-SNAPSHOT.jar"]