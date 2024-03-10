package fi.jannetahkola.palikka.game.process;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * Regexes for the various logs that the game process outputs.
 */
@UtilityClass
public class GameProcessPatterns {
    public static final String BASE_SERVER_THREAD_REGEXP = "^\\[\\d{2}:\\d{2}:\\d{2}] \\[Server thread/INFO]:";
    public static final Pattern SERVER_START_PATTERN = Pattern.compile(BASE_SERVER_THREAD_REGEXP + " Done.*\"$");
}
