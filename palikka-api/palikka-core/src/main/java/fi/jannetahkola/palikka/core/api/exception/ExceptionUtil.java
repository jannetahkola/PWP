package fi.jannetahkola.palikka.core.api.exception;

public class ExceptionUtil {
    ExceptionUtil() {
        // Util
    }

    public static Throwable getOriginalCause(Throwable e) {
        if (e == null) return null;
        Throwable cause = e.getCause();
        if (cause == null) {
            return e;
        }
        return getOriginalCause(cause);
    }
}
