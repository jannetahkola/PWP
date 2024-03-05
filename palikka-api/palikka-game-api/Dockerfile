FROM eclipse-temurin:21
WORKDIR /

USER root

RUN apt-get update && \
    apt-get -y install sudo

RUN adduser --disabled-password --gecos '' minecraft \
    && adduser minecraft sudo \
    && echo '%sudo ALL=(ALL:ALL) ALL' >> /etc/sudoers

RUN mkdir -p /home/minecraft/minecraft
RUN chown minecraft:minecraft /home/minecraft/minecraft

USER minecraft

COPY ["target/palikka-game-api-0.0.1-SNAPSHOT.jar", "/home/minecraft/app.jar"]
EXPOSE 80

ENTRYPOINT ["java", "-jar", "/home/minecraft/app.jar"]