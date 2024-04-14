package fi.jannetahkola.palikka.users.api.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.validation.annotation.Validated;

@Schema(description =
        "Request object containing the identifier of the role " +
                "that should be associated with the user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserRolePostModel {
    @Schema(description = "Identifier of the role to associate with the user", example = "1")
    @NotNull
    @JsonProperty("role_id") // Needed for HAL-FORMS affordance, naming strategy not enough
    Integer roleId;
}
