# PWP SPRING 2024
# Game Management API
# Group information
* Student 1. Janne Tahkola jantahko@student.oulu.fi

__Remember to include all required documentation and HOWTOs, including how to create and populate the database, how to run and test the API, the url to the entrypoint and instructions on how to setup and run the client__

# API
Located under `palikka-api/`. Loosely based on the [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) principle.
All implementation has been done on Ubuntu and macOS for Linux based hosts -> can't guarantee compatibility with other platforms.

## Dependencies
- Java 21+
- Docker 24.0.7+
- Docker Compose 2.23.3+
- (Optional) Maven 3.9.5+
- (Optional) Make 4.3+

## Running
### Make
todo

### Without Make
Run the commands from `Makefile` independently.

## Development

`keytool -genkeypair -alias jwt -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore dev.keystore`

WireMock requires __files: https://wiremock.org/docs/stubbing/#specifying-the-response-body