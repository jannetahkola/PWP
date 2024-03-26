package fi.jannetahkola.palikka.game.api.game.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameOutputMessage {
    @NotBlank
    @Pattern(regexp = "^((/[a-z])|([a-z])).*$") // Must start with /+letter or just a letter
    private String data;
}
