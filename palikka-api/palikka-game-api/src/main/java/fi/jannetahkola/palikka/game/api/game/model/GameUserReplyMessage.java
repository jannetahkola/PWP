package fi.jannetahkola.palikka.game.api.game.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameUserReplyMessage {
    @NotNull
    private Type typ;

    @NotBlank
    private String data;

    public enum Type {
        ERROR("err"),

        /**
         * The message contains the full input history in a single String, each input entry separated by a new line.
         * Only sent once when subscribing.
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
