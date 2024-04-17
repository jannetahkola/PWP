# Palikka API

## Dependencies
- Docker 25.0+
- Docker Compose 2.24+
- (Optional) Make 3.81+
  - Requires also some common Linux shell such as Bash or Zsh

## Project structure
The multi-module project consists of the following submodules:

- [palikka-core](palikka-core)
  - Library that contains code that is shared between the services
- [palikka-game-api](palikka-game-api)
  - Service for managing the game executable and its files
  - Requires authentication via the users API
- [palikka-users-api](palikka-users-api)
  - Service for authenticating and managing users, roles and privileges
- [palikka-mock-file-server](palikka-mock-file-server)
  - Serves a manually downloaded game executable (.jar) for local testing
  - Avoids constant calls to Mojang's servers
  - **Not a production service**

## Local deployment
If you don't use Make, you can run the commands from [Makefile](Makefile) individually. Note that some of them utilise 
shell commands. Run `make` to get descriptions of available Make targets.

> All implementation has been done on Ubuntu or macOS targeting Linux based hosts -> no guarantees on compatibility 
> with other platforms.

1. Create and run the PostgreSQL container, initialize schema and insert seed data
    ```shell
    make init-data
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

## Testing
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
code quality analysis. It is enabled only in "production environment".

1. Create and run the PostgresSQL container
    ```shell
    make init-data
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