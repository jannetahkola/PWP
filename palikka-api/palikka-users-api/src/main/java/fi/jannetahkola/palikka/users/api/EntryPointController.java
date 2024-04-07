package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.users.api.auth.AuthenticationController;
import fi.jannetahkola.palikka.users.api.user.CurrentUserController;
import fi.jannetahkola.palikka.users.api.user.UserController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Entry point")
@RestController
@RequestMapping("/")
public class EntryPointController {
    @Operation(
            summary = "Get the entry point for the API",
            description = "Response contains suggested starting links for navigating the API")
    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @SuppressWarnings("squid:S1452") // No model type, links only
    public ResponseEntity<RepresentationModel<?>> getLinks() {
        return ResponseEntity.ok()
                .body(new RepresentationModel<>().add(
                        linkTo(methodOn(EntryPointController.class).getLinks())
                                .withSelfRel(),
                        linkTo(methodOn(AuthenticationController.class).login(null))
                                .withRel("login"),
                        linkTo(methodOn(UserController.class).getUsers())
                                .withRel("users"),
                        linkTo(methodOn(CurrentUserController.class).getCurrentUser(null))
                                .withRel("current_user")
                ));
    }
}
