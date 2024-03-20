package fi.jannetahkola.palikka.game.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    ProcessProperties process = new ProcessProperties();

    @NotNull
    FileProperties file;

    @Data
    @Valid
    public static class ProcessProperties {
        /**
         * Duration of the game process stop time out. Defaults to 10 seconds.
         */
        @NotNull
        Duration stopTimeout = Duration.ofSeconds(10);
    }

    @Data
    @Valid
    public static class StatusProperties {
        /**
         * The public host the game executable is running on. Returned in the
         * status response as the host name - in reality this does not affect the status request
         * as it is always done to <code>localhost</code>. E.g. myserver.com.
         */
        @NotBlank
        String host;

        /**
         * Port that the game executable accepts connections on. Defaults to 25565.
         */
        @NotNull
        int port = 25565;

        /**
         * Duration of the status request connection timeout. Defaults to 2 seconds.
         */
        @NotNull
        Duration connectTimeout = Duration.ofSeconds(2);
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
        @Pattern(regexp = "^.*(\\.jar)$")
        String name;

        /**
         * Command to execute the game JAR file, e.g. java -jar server.jar.
         */
        @NotBlank
        @Pattern(regexp = "^(java -jar).*(\\.jar)$")
        String startCommand;

        public Path getPathToJarFileDirectory() {
            return Paths.get(path);
        }

        public Path getPathToJarFile() {
            return Paths.get(path, name);
        }
    }
}
