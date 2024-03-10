package fi.jannetahkola.palikka.game.api.game.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameMessage {
    private Source src;

    private Type typ;

    @NotBlank
    private String data;

    public enum Source {
        /**
         * The message originates from the API server.
         */
        SERVER("srv"),

        /**
         * The message originates from the game process.
         */
        GAME("gm");

        final String value;

        @JsonCreator
        Source(@NonNull String value) {
            this.value = value.toLowerCase(Locale.ROOT);
        }

        @JsonValue
        public String getValue() {
            return this.value.toLowerCase(Locale.ROOT);
        }
    }

    public enum Type {
        /**
         * The message contains a new input from the game process.
         */
        LOG("log"),

        /**
         * The message contains the full input history in a single String, each input entry separated by a new line.
         * Only sent once when subscribing. Message source will be {@link Source#SERVER}.
         */
        HISTORY("hist");

        final String value;

        @JsonCreator
        Type(@NonNull String value) {
            this.value = value.toLowerCase(Locale.ROOT);
        }

        @JsonValue
        public String getValue() {
            return this.value.toLowerCase(Locale.ROOT);
        }
    }
}
