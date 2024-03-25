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
public class Role {
    @NotNull
    private Integer id;

    @NotBlank
    private String name;

    private Set<Privilege> privileges;

    public Optional<Set<Privilege>> getPrivileges() {
        return privileges != null ? Optional.of(privileges) : Optional.empty();
    }
}