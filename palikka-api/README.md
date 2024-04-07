# Palikka API

## Local deployment

1. Create and run the PostgreSQL container, initialize schema and insert seed data
    ```shell
    make init-data
    ```
2. Build the services (omit `test=0` to run tests)
    ```shell
    make init-data
    ```
3. Create and run the service containers
    ```shell
    make docker-run
    ```

Once the containers are up, see:
- OpenAPI documentation at [http://localhost:8080/users-api/swagger-ui/index.html](http://localhost:8080/users-api/swagger-ui/index.html)
- Entry point at [http://localhost:8080/users-api/](http://localhost:8080/users-api/)

## Code analysis

[SonarQube](https://www.sonarsource.com/products/sonarqube/) is available as an additional Docker service for 
code analysis.

1. Create and run the PostgreSQL & SonarQube containers
    ```shell
    docker-compose up -d postgres sonarqube
    docker-compose logs -f
    ```
2. Navigate to [localhost:9000](http://localhost:9000)
3. Login with `admin/admin` and change the password
4. Generate a user token in `My Account > Security` and copy it
5. Run tests and upload the reports to SonarQube:
    ```shell
    ./mvnw clean verify sonar:sonar -Dsonar.login=<my_user_token>
    ```
   or if you omitted `test=0` during deployment, you can just upload the 
   existing reports:
   ```shell
   ./mvnw sonar:sonar -Dsonar.login=<my_user_token>
   ```
6. Navigate to `Projects` from the SonarQube menu to see the report