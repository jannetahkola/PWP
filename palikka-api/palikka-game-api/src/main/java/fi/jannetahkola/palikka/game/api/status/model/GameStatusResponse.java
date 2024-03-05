package fi.jannetahkola.palikka.game.api.status.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameStatusResponse {
    private boolean online = true;
    private String version;
    private String description;
    private String motd;
    private Players players;
    private String favicon;
    private String host;
    private int port;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Players {
        private Integer max;
        private Integer online;
    }

    @SuppressWarnings("unchecked")
    @JsonProperty("version")
    private void unpackVersion(Object o) {
        if (o != null) {
            if (o instanceof String unpackedVersion) {
                // Deserializing internally
                this.version = unpackedVersion;
            } else if (o instanceof Map) {
                try {
                    // Deserializing a response from a game sever
                    Map<String, Object> temp = (Map<String, Object>) o;
                    this.version = (String) temp.get("name");
                } catch (ClassCastException e) {
                    // No care in the world
                }
            }
        }
    }

}
