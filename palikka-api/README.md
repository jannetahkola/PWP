# Palikka API

## Project structure
The multi-module project consists of the following submodules:

- [palikka-core](palikka-core)
  - Library for shared code between the services
- [palikka-users-api](palikka-users-api)
  - Service for authenticating and managing users, roles and privileges
- [palikka-game-api](palikka-game-api)
  - Service for managing the game executable and its files
  - Requires authentication via the users API
- [palikka-mock-file-server](palikka-mock-file-server)
  - Serves a manually downloaded game executable (.jar) for local testing
  - Avoids constant calls to Mojang's servers
  - **_Not a production service!_**

All of the above modules inherit the [parent POM](pom.xml), which provides a few common dependencies as well as
dependency version management.

## Local deployment

### General
Local deployments utilize Dockerfiles and Docker Compose. Each API service has its own Dockerfile, and all of these 
are managed by a single Compose file (see [docker-compose.yaml](docker-compose.yaml)). PostgreSQL and SonarQube 
have separate Compose files.

Before the service images are built, a few other Dockerfiles are executed when using the provided Makefile

- a common [deps.Dockerfile](deps.Dockerfile) that installs all dependencies for the API services
- a [mode](#modes)-dependent, common Dockerfile for the core library that installs the root POM and the core library itself (see [Dockerfiles](/conf/dockerfiles))

Results from the above are reused when building images for the API services.

### Modes
Local deployment can be done either in development or production mode. The difference can be seen by looking at the 
[Dockerfiles](/conf/dockerfiles) - development will use files suffixed with `.dev.Dockerfile`. This section describes the development mode.

See [Code Analysis](#code-analysis) for using production mode. The main difference is that builds in production 
mode run tests and require SonarQube to provide code quality analysis, which makes the builds slower.

### Dependencies
- Docker 25.0+
- Docker Compose 2.24+
- (Optional) GNU Make 3.81+
    - Requires also some common Linux shell such as Bash or Zsh

If you don't use Make, you can run the commands from [Makefile](Makefile) individually. Note that some of them utilise 
shell commands. Run `make` to get descriptions of available Make targets.

> All implementation has been done on Ubuntu or macOS targeting Linux based hosts -> no guarantees on compatibility 
> with other platforms.

1. Create and run the PostgreSQL container, initialize schema and insert seed data
    ```shell
    make init-postgres
    ```
2. Setup local environment for development. Provides faster deployments by skipping tests and code quality analysis
    ```shell 
    make env-dev
    ```
3. Create and run the service containers
    ```shell
    make docker-run
    ```

Once the containers are up, see:
- OpenAPI documentation at [http://localhost:8080/users-api/swagger-ui/index.html](http://localhost:8080/users-api/swagger-ui/index.html)
- Entry point at [http://localhost:8080/users-api/](http://localhost:8080/users-api/)
- [Deploying the Palikka Client](../palikka-client/README.md)

## Local development
### Dependencies
- Docker 25.0+
- Docker Compose 2.24+
- Java 21+
- (Optional) GNU Make 3.81+
    - Requires also some common Linux shell such as Bash or Zsh

### Testing
Unit and integration tests for each module are under `/{module_name}/src/test/`. Run the whole test suite from the root directory ([palikka-api](./)) with
```shell
./mvnw clean verify
```

Database and test data are handled automatically. External libraries used in testing:
- [REST Assured](https://rest-assured.io)
  - REST API testing library
- [WireMock](https://wiremock.org)
  - Mock responses from downstream APIs
- [Testcontainers](https://testcontainers.com)
  - Manage a Docker-based PostgreSQL database
- [Embedded Redis](https://github.com/codemonstur/embedded-redis)
  - Manage an embedded Redis server

## Code analysis

[SonarQube](https://www.sonarsource.com/products/sonarqube/) is available as an additional Docker service for 
code quality analysis. It is enabled only in production mode when using Docker deployment. It is configured to use 
the PostgreSQL container as its database, thus Postgres needs to be up before the SonarQube container.

1. Create and run the PostgresSQL container
    ```shell
    make init-postgres
    ```
2. Create and run the SonarQube container
    ```shell
    make init-sonar
    ```
   This does a few additional things on the first run via the SonarQube Web API
    - Changes the default password to avoid SonarQube prompting it on login
    - Generates a user token and stores it into a new file called `sonar.txt`
3. Setup local environment for production. Runs all tests and uploads code quality reports to SonarQube
   using `sonar.txt` from the previous step
    ```shell 
    make env-prod
    ```
4. Run the service containers
    ```shell
    make run-docker
    ```
5. Go to [http://localhost:9000](http://localhost:9000) and login using `admin/pass`
6. Navigate to Projects to see the reports