package fi.jannetahkola.palikka.users.api.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Locale;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserRolePatchModel {
    @NotNull
    @Size(min = 1)
    @Singular
    Set<UserRolePatch> patches;

    @Value
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class UserRolePatch {
        @NotNull
        Action action;

        @NotNull
        Integer roleId;
    }

    public enum Action {
        ADD("add"),
        DELETE("delete");

        final String value;

        Action(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return this.value.toLowerCase(Locale.ROOT);
        }

        @JsonCreator
        public static Action fromValue(String value) {
            return Action.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
