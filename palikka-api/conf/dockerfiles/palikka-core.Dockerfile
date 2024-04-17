FROM maven:3.9 AS build-core-with-deps

WORKDIR /app
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /app/ /app
COPY ../../palikka-core/src /app/palikka-core/src

# Install parent POM in non-recursive mode
RUN mvn -B -e -N clean install

WORKDIR /app/palikka-core
COPY ../../sonar.txt /app/palikka-core/sonar.txt
COPY ../../lombok.config /app/palikka-core/lombok.config
RUN mvn -B -e clean install \
    sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login="$(cat sonar.txt)"