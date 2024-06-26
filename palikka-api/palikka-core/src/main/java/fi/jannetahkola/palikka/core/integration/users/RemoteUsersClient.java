package fi.jannetahkola.palikka.core.integration.users;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.core.config.properties.RemoteUsersIntegrationProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.client.Hop.rel;

@Slf4j
public class RemoteUsersClient implements UsersClient {
    private static final Validator VALIDATOR;

    static {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = validatorFactory.getValidator();
        }
    }

    private final RemoteUsersIntegrationProperties properties;
    private final JwtService jwtService;

    public RemoteUsersClient(RemoteUsersIntegrationProperties properties,
                             JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    @Override
    public User getUser(Integer userId) {
        log.debug(">> GET user, user id={}", userId);

        try {
            URI baseUri = properties.getBaseUri().resolve("/users-api/users");
            HttpHeaders httpHeaders = new HttpHeaders();
            jwtService.sign(new JWTClaimsSet.Builder(), PalikkaJwtType.SYSTEM)
                    .ifPresent(signedJWT -> httpHeaders.setBearerAuth(signedJWT.serialize()));
            log.debug(">> GET user - Bearer auth present={}", httpHeaders.containsKey(HttpHeaders.AUTHORIZATION));

            var modelType = new TypeReferences.EntityModelType<User>() {};
            Traverson traverson = new Traverson(baseUri, MediaTypes.HAL_JSON);
            EntityModel<User> userModel = traverson
                    .follow(rel("item").withParameter("id", userId))
                    .withHeaders(httpHeaders)
                    .toObject(modelType);

            if (userModel == null) return null;

            User user = userModel.getContent();
            Set<ConstraintViolation<User>> violations = VALIDATOR.validate(user);

            if (violations.isEmpty()) {
                log.debug("<< GET user - ok");
                return user;
            }

            log.warn("<< GET user - response has constraint violations={}", violations);
        } catch (Exception e) {
            log.error("<< GET user - request failed on exception", e);
        }

        return null;
    }

    @Override
    public Collection<Role> getUserRoles(Integer userId) {
        log.debug(">> GET user roles, user id={}", userId);

        try {
            URI baseUri = properties.getBaseUri().resolve("/users-api/users");
            HttpHeaders httpHeaders = new HttpHeaders();
            jwtService.sign(new JWTClaimsSet.Builder(), PalikkaJwtType.SYSTEM)
                    .ifPresent(signedJWT -> httpHeaders.setBearerAuth(signedJWT.serialize()));
            log.debug(">> GET user roles - Bearer auth present={}", httpHeaders.containsKey(HttpHeaders.AUTHORIZATION));

            var modelType = new TypeReferences.CollectionModelType<Role>() {};

            Traverson traverson = new Traverson(baseUri, MediaTypes.HAL_JSON);
            CollectionModel<Role> roleCollectionModel = traverson
                    .follow(rel("item").withParameter("id", userId))
                    .follow("user_roles")
                    .withHeaders(httpHeaders)
                    .toObject(modelType);

            if (roleCollectionModel == null) return Collections.emptyList();

            Collection<Role> roleCollection = roleCollectionModel.getContent();

            // todo figure out why validation doesn't work here for nested collections
            Set<ConstraintViolation<Collection<Role>>> roleViolations = VALIDATOR.validate(roleCollection);
            Set<ConstraintViolation<Privilege>> privilegeViolations = roleCollection.stream()
                    .map(role -> role.getPrivileges().stream()
                            .map(privilege -> VALIDATOR.validate(privilege))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet()))
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            if (roleViolations.isEmpty() && privilegeViolations.isEmpty()) {
                log.debug("<< GET user roles - ok");
                return roleCollection;
            }

            log.warn("<< GET user roles - response has constraint violations, " +
                    "roles={}, privileges={}", roleViolations, privilegeViolations);
        } catch (Exception e) {
            log.error("<< GET user roles - request failed on exception", e);
        }

        return Collections.emptyList();
    }
}
