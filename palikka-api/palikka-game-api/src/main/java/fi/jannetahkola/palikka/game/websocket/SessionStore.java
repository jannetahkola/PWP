package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Additional session store to mitigate the issue mentioned in {@link PreSendAuthorizationChannelInterceptor}.
 */
@Slf4j
@RequiredArgsConstructor
public class SessionStore {
    private final JwtService jwtService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(new HashMap<>());

    public int sessionCount() {
        return sessions.size();
    }

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("Session stored with id={}", session.getId());
    }

    public void removeSession(String sessionId) {
        try (var removedSession = sessions.remove(sessionId)) {
            log.debug("Session removed with id={}", sessionId);
        } catch (IOException e) {
            log.error("Failed to remove WebSocketSession", e);
        }
    }

    /**
     * Goes through the sessions map to find any session established with a now-expired JWT. All such
     * sessions are closed (disconnected) and removed from the map.
     */
    public void evictExpiredSessions() {
        log.info("Evicting expired sessions");
        int count = 0;
        for (WebSocketSession session : sessions.values()) {
            var authenticationToken = (UsernamePasswordAuthenticationToken) session.getPrincipal();
            if (authenticationToken != null && jwtService.isExpired((String) authenticationToken.getDetails())) {
                if (!session.isOpen()) {
                    log.warn("Removing a stale closed session with an expired " +
                            "JWT, session id={}, principal={}", session.getId(), authenticationToken.getName());
                    removeSession(session.getId());
                    continue;
                }
                try {
                    session.close();
                    log.debug("Closed session with expired JWT, " +
                            "session id={}, principal={}", session.getId(), authenticationToken.getName());
                } catch (IOException e) {
                    log.error("Failed to close session id={}", session.getId());
                } finally {
                    removeSession(session.getId());
                    count++;
                }
            }
        }
        log.info("Evicted {} expired session(s)", count);
    }
}
