package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.properties.RemoteUsersIntegrationProperties;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteUsersClientIT extends IntegrationTest {
    @Test
    void testRemoteUsersClientHasAccessToRequiredEndpoints(@Autowired JwtService jwtService) {
        RemoteUsersIntegrationProperties props = new RemoteUsersIntegrationProperties();
        props.setBaseUri(URI.create(RestAssured.baseURI + ":" + RestAssured.port));
        RemoteUsersClient remoteUsersClient = new RemoteUsersClient(props, jwtService);

        User user = remoteUsersClient.getUser(1);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1);

        Collection<Role> userRoles = remoteUsersClient.getUserRoles(1);
        assertThat(userRoles).isNotEmpty();
    }
}
