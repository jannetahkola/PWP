package fi.jannetahkola.palikka.game.util;

import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import jakarta.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class GameCommandUtil {
    private static final String COMMAND_AUTHORITY_PREFIX = "COMMAND_";

    /**
     * Normalizes the given command into it's root. The first characters of parameter must be validated beforehand
     * like in {@link GameOutputMessage#getData()}.
     * <br>
     *
     * Example:
     * <pre>{@code
     * GameCommandUtil.normalizeCommand("/weather clear") // -> weather
     * GameCommandUtil.normalizeCommand("wEather clear") // -> weather
     * }</pre>
     *
     * @param rawCommandWithArgs Raw command input including arguments, e.g. /weather clear
     * @return Normalized command
     */
    public String normalizeCommand(@Nonnull String rawCommandWithArgs) {
        String command = rawCommandWithArgs
                .trim()
                .split(" ")[0];

        if (command.indexOf("/") == 0) {
            // Remove starting slash if present
            command = command.substring(1);
        }

        return command.toLowerCase(Locale.ROOT);
    }

    /**
     * Authorizes the current authentication token for the given command by the user's authorities. To be
     * authorized, the token must have a COMMAND_ prefixed authority matching the command, and the
     * command must be normalized.
     * <br>
     *
     * Example:
     * <pre>{@code
     * var authentication = new UsernamePasswordToken(1, null, List.of(new SimpleGrantedAuthority("COMMAND_weather")));
     * GameCommandUtil.authorizeCommand(authentication, "weather") // -> true
     *
     * var authentication = new UsernamePasswordToken(1, null, List.of(new SimpleGrantedAuthority("COMMAND_op")));
     * GameCommandUtil.authorizeCommand(authentication, "weather") // -> false
     * }</pre>
     *
     * @param usersClient User integration client implementations
     * @param authentication Current authentication
     * @param normalizedCommand The normalized command to authorize
     * @return True if authorized, false otherwise
     */
    public boolean authorizeCommand(UsersClient usersClient,
                                    Authentication authentication,
                                    String normalizedCommand) {
        Collection<Role> userRoles = usersClient.getUserRoles(Integer.valueOf(authentication.getName()));
        Set<GrantedAuthority> userPrivileges = userRoles.stream()
                .map(role -> role.getPrivileges().stream()
                        .map(privilege -> new SimpleGrantedAuthority(privilege.getDomain() + "_" + privilege.getName()))
                        .collect(Collectors.toSet()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        return userPrivileges.stream()
                .filter(grantedAuthority -> grantedAuthority.getAuthority().startsWith(COMMAND_AUTHORITY_PREFIX))
                .anyMatch(grantedAuthority -> {
                    // Use substring() since we know the index from the prefix, and it's faster than split()
                    String authorizedCommand = grantedAuthority.getAuthority().substring(8);
                    if (authorizedCommand.startsWith(normalizedCommand.split(" ")[0])) {
                        log.debug("Authorized access to command '{}' with authority '{}' for principal '{}'",
                                normalizedCommand, authorizedCommand, authentication.getName());
                        return true;
                    }
                    return false;
                });
    }
}
