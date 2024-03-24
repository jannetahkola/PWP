package fi.jannetahkola.palikka.core.auth;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PalikkaAuthenticationDetails implements Serializable {
    @Serial
    private static final long serialVersionUID = 2927250142344547083L;

    private String token;
}
