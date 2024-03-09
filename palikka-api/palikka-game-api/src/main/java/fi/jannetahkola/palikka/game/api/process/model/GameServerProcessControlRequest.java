package fi.jannetahkola.palikka.game.api.process.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Locale;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameServerProcessControlRequest {
    @NotNull
    Action action;

    // TODO Use string instead of enum for requests
    public enum Action {
        START("start"),
        STOP("stop");

        final String value;

        Action(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Action fromValue(String value) {
            return Action.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
