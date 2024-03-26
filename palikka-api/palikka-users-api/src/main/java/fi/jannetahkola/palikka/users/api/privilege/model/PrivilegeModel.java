package fi.jannetahkola.palikka.users.api.privilege.model;

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

@Schema(description = "Role privilege")
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Relation(itemRelation = "privilege", collectionRelation = "privileges")
public class PrivilegeModel extends RepresentationModel<PrivilegeModel> {
    @Schema(description = "Identifier of the privilege", example = "1234")
    @NotNull
    Integer id;

    @Schema(description = "Category of the privilege", example = "COMMAND")
    @NotBlank
    String category;

    @Schema(description = "Name of the privilege", example = "weather")
    @NotBlank
    String name;
}
