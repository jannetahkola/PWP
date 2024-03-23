package fi.jannetahkola.palikka.users.api.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Relation(itemRelation = "user", collectionRelation = "users")
public class UserModel extends RepresentationModel<UserModel> {

    Integer id;

    @NotBlank(groups = {PostGroup.class, PutGroup.class}) // TODO Add @Size and @SafeText
    String username;

    @NotBlank(groups = PostGroup.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password;

    @NotNull(groups = {PostGroup.class})
    Boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Boolean root;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    OffsetDateTime lastUpdatedAt;

    @Builder.Default
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Set<String> roles = new HashSet<>();

    public interface PostGroup {}

    public interface PutGroup {}
}
