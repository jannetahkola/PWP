package fi.jannetahkola.palikka.users.api.user.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Relation(itemRelation = "user", collectionRelation = "users")
public class UserModel extends RepresentationModel<UserModel> {

    @Schema(description = "Identifier of the user", example = "1234")
    Integer id;

    @Schema(description = "Unique username of the user. Required when creating or updating a user")
    @NotBlank(groups = {PostGroup.class, PutGroup.class})
    @Pattern(regexp = "^[a-zA-Z\\d-]{6,20}$", groups = {PostGroup.class, PutGroup.class})
    String username;

    @Schema(description = "Password of the user. Required when creating a user")
    @NotBlank(groups = PostGroup.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Pattern(regexp = "^[^\\s]{6,20}$", groups = {PostGroup.class, PutGroup.class})
    String password;

    @Schema(description = "Whether the user is active, i.e. they can log in. Required when creating a user")
    @NotNull(groups = {PostGroup.class})
    Boolean active;

    @Schema(description = "Whether the user is a root user, i.e. not modifiable")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Boolean root;

    @Schema(description = "When the user was created")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime createdAt;

    @Schema(description = "When the user was last updated")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    OffsetDateTime lastUpdatedAt;

    @Schema(description = "Roles that the user has")
    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Set<String> roles = new HashSet<>();

    public interface PostGroup {}

    public interface PutGroup {}
}
