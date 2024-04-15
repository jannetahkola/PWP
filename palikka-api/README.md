# Palikka API

## Dependencies
- Docker 25.0+
- Docker Compose 2.24+
- Java 21
- (Optional) Make 3.81+

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
- [coverage-reporting](coverage-reporting)
  - Utility module for aggregating coverage reports as described in the [SonarSource multimodule example](https://github.com/SonarSource/sonar-scanning-examples/tree/master/sonar-scanner-maven/maven-multimodule)

**The current project structure presents a problem with the Docker deployment model**: each service is run from its 
own Dockerfile that resides in the corresponding submodule. Because they depend on code that's not inside submodule itself, 
such as the core library, the whole project has to be built on the host before the Docker containers are started. 
The individual Dockerfiles cannot copy resources from parent directories for security reasons. This is the reason why 
the Java dependency is present at all.

One way to solve this would be to serve the core library from a remote package repository.

## Local deployment
If you don't use Make, you can run the commands from [Makefile](Makefile) individually. Note that some of them utilise 
shell commands.

> All implementation has been done on Ubuntu or macOS targeting Linux based hosts -> no guarantees on compatibility 
> with other platforms.

1. Create and run the PostgreSQL container, initialize schema and insert seed data
    ```shell
    make init-data
    ```
2. Build the services (use `build-test` to also run tests and create code quality reports)
    ```shell
    make build
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
make build-test
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
code quality analysis.

1. Create and run the PostgreSQL and SonarQube containers
    ```shell
    make docker-run-sonar
    ```
    This does a few additional things on the first run
    - Changes the default password to avoid SonarQube prompting it on login
    - Generates a user token and stores it into a new file called `sonar.txt`
2. Run tests and create reports. Optional if you used `build-test` during deployment
    ```shell
   make build-test
    ```
3. Upload the reports to SonarQube using the user token from `sonar.txt`
    ```shell
    make sonar-report
    ```
4. Go to [http://localhost:9000](http://localhost:9000) and login using `admin/pass`
5. Navigate to Projects to see the full report