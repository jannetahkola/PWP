FROM maven:3.9 AS build-core-with-deps

WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /app/ /app
COPY ../../palikka-core/src /app/palikka-core/src

RUN mvn -B -e clean install -DskipTests -pl '.,palikka-core'