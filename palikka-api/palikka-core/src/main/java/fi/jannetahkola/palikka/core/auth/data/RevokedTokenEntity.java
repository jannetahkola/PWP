package fi.jannetahkola.palikka.core.auth.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash
@Getter
@Setter
public class RevokedTokenEntity {
    @Id
    @NotBlank
    private String tokenId;

    @TimeToLive
    @NotNull
    private Long ttlSeconds;
}
