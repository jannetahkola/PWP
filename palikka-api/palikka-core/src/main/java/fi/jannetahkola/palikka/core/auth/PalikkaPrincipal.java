package fi.jannetahkola.palikka.core.auth;

import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

@Getter
@ToString
public class PalikkaPrincipal implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 210969667929041714L;

    private final Integer id;
    private final String username;

    public PalikkaPrincipal(Integer id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public String getName() {
        return String.valueOf(this.id);
    }
}
