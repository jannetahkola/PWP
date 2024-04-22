FROM maven:3.9 AS deps

WORKDIR /app
COPY palikka-core/pom.xml palikka-core/pom.xml
COPY palikka-users-api/pom.xml palikka-users-api/pom.xml
COPY palikka-game-api/pom.xml palikka-game-api/pom.xml
COPY palikka-mock-file-server/pom.xml palikka-mock-file-server/pom.xml
COPY pom.xml .

# -B: Run in non-interactive (batch) mode, i.e. no output colors
# -e: Produce execution error messages
# -C: Fail the build if checksums don’t match
# maven-dependecy-plugin:go-offline: Tells Maven to resolve everything this project is dependent on (dependencies, plugins, reports) in preparation for going offline
# -DexcludeArtifactIds: Exclude some modules from above
RUN mvn -B -e -C \
    org.apache.maven.plugins:maven-dependency-plugin:3.6.1:go-offline