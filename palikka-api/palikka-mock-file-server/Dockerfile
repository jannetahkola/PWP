FROM eclipse-temurin:21

RUN mkdir -p /app
WORKDIR /app

COPY ["target/mock-game-file-server-0.0.1-SNAPSHOT.jar", "app.jar"]
COPY ["entrypoint.sh", "/entrypoint.sh"]
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]