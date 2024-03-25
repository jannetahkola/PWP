package fi.jannetahkola.palikka.core.integration.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
public class Privilege {
    @NotNull
    private Integer id;

    @NotBlank
    private String category;

    @NotBlank
    private String name;
}
