package fi.jannetahkola.palikka.users.api.role.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Authorizes associated users to access different APIs")
@Value
@Builder
@EqualsAndHashCode(callSuper = true, exclude = "privileges")
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

    @Schema(description = "Privileges associated with the role")
    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    List<PrivilegeModel> privileges = new ArrayList<>();
}
