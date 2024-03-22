package fi.jannetahkola.palikka.core.auth.jwt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PalikkaJwtType {
    USER("usr"), SYSTEM("sys");

    final String value;

    PalikkaJwtType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value.toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static PalikkaJwtType fromValue(String value) {
        return PalikkaJwtType.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
