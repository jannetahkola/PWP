package fi.jannetahkola.palikka.users.api.role.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Schema(description = "User role")
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Relation(itemRelation = "role", collectionRelation = "roles")
public class RoleModel extends RepresentationModel<RoleModel> {
    @Schema(description = "Identifier of the role", example = "1234")
    @NotNull
    Integer id;

    @Schema(description = "Unique name of the role", example = "ROLE_USER")
    @NotBlank
    String name;

    @Schema(description = "Description of the role", example = "Access to limited functionality")
    String description;
}
