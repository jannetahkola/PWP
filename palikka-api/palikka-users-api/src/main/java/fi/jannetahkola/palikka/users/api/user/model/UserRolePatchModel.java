package fi.jannetahkola.palikka.users.api.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;
import java.util.Set;

@Schema(description = "Request object containing the desired patch operations to a user's role associations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserRolePatchModel {
    @Schema(description = "The patch operations to apply to a user's role associations")
    @NotNull
    @Size(min = 1, max = 20)
    @Singular
    @Valid // This must be here instead of the model below
    Set<UserRolePatch> patches;

    @Schema(description = "An individual patch operation")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class UserRolePatch {
        @Schema(description = "The action to apply", example = "add")
        @NotNull
        Action action;

        @Schema(description = "Identifier of the target role", example = "1234")
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
