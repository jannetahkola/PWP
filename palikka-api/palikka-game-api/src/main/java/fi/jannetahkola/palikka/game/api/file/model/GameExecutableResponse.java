package fi.jannetahkola.palikka.game.api.file.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GameExecutableResponse {
    String configuredPath;

    boolean exists;

    @JsonProperty("is_file") // "is" gets removed otherwise
    boolean isFile;

    long fileSizeMB;
}
