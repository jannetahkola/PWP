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

@Schema(description = "Authorizes users with associated roles to perform various actions within APIs")
@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Relation(itemRelation = "privilege", collectionRelation = "privileges")
public class PrivilegeModel extends RepresentationModel<PrivilegeModel> {
    @Schema(description = "Identifier of the privilege", example = "1234")
    @NotNull
    Integer id;

    @Schema(
            description = "Domain of the privilege. References an action inside an API this privilege is applied within",
            example = "COMMAND")
    @NotBlank
    String domain;

    @Schema(
            description = "Name of the privilege. Provides more fine grained access control within a domain",
            example = "weather")
    @NotBlank
    String name;

    @Schema(
            description = "An optional text to describe the privilege in its domain",
            example = "Adds IP address to banlist.")
    String domainDescription;
}
