package fi.jannetahkola.palikka.users.api.role.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.validation.annotation.Validated;

@Schema(description =
        "Request object containing the identifier of the privilege " +
                "that should be associated with the role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Validated
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RolePrivilegePostModel {
    @Schema(description = "Identifier of the privilege to associate with the role", example = "1")
    @NotNull
    @JsonProperty("privilege_id") // Needed for HAL-FORMS affordance, naming strategy not enough
    Integer privilegeId;
}

