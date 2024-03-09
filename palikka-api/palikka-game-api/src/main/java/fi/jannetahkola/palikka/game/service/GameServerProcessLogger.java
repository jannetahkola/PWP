package fi.jannetahkola.palikka.game.service;

import lombok.extern.slf4j.Slf4j;

/**
 * Separate class for logging game server process logs. Makes it easier to separate them.
 */
@Slf4j
public class GameServerProcessLogger {
    public void log(String input) {
        log.info(input);
    }
}
