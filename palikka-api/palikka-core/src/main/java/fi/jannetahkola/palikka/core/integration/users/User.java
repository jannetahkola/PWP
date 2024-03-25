package fi.jannetahkola.palikka.core.integration.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
public class User {
    @NotNull
    private Integer id;

    @NotBlank
    private String username;

    @NotNull
    private Boolean active;

    @NotNull
    private Boolean root;

    private Set<String> roles;

    public Optional<Set<String>> getRoles() {
        return roles != null ? Optional.of(roles) : Optional.empty();
    }
}
