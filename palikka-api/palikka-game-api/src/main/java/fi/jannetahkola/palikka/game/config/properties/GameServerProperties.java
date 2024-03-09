package fi.jannetahkola.palikka.game.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@ConfigurationProperties("palikka.game.server")
@Validated
public class GameServerProperties {
    @NotBlank
    String host;

    @NotNull
    int port;

    @NotNull
    long stopTimeoutInMillis;

    @NotNull
    FileProperties file;

    @Data
    @Valid
    public static class FileProperties {
        /**
         * Path to the directory where the game server file is located, e.g. /home/me/minecraft.
         */
        @NotBlank
        String path;

        /**
         * Name of the game server file, e.g. server.jar.
         */
        @NotBlank
        String name;

        /**
         * Command to execute the game server file, e.g. java -jar server.jar.
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
