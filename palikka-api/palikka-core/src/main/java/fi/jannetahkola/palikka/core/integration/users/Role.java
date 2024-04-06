package fi.jannetahkola.palikka.core.integration.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
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

    @Valid
    private Set<Privilege> privileges = new HashSet<>();
}
