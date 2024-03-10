package fi.jannetahkola.palikka.game.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Data
@ConfigurationProperties("palikka.game")
@Validated
public class GameProperties {
    @NotNull
    StatusProperties status;

    @NotNull
    ProcessProperties process;

    @NotNull
    FileProperties file;

    @Data
    @Valid
    public static class ProcessProperties {
        @NotNull
        Duration stopTimeout;
    }

    @Data
    @Valid
    public static class StatusProperties {
        @NotBlank
        String host;

        @NotNull
        int port;
    }

    @Data
    @Valid
    public static class FileProperties {
        /**
         * Path to the directory where the game JAR file is located, e.g. /home/me/minecraft.
         */
        @NotBlank
        String path;

        /**
         * Name of the game JAR file, e.g. server.jar.
         */
        @NotBlank
        String name;

        /**
         * Command to execute the game JAR file, e.g. java -jar server.jar.
         */
        @NotBlank // todo add more validations
        String startCommand;

        public Path getPathToJarFileDirectory() {
            return Paths.get(path);
        }

        public Path getPathToJarFile() {
            return Paths.get(path, name);
        }
    }
}
