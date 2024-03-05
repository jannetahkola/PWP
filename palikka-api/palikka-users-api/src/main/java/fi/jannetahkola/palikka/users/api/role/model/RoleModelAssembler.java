package fi.jannetahkola.palikka.users.api.role.model;

import fi.jannetahkola.palikka.users.api.role.RoleController;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import jakarta.annotation.Nonnull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class RoleModelAssembler implements RepresentationModelAssembler<RoleEntity, RoleModel> {
    @Override
    @Nonnull
    public RoleModel toModel(RoleEntity entity) {
        RoleModel roleModel = RoleModel.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();

        roleModel.add(WebMvcLinkBuilder.linkTo(methodOn(RoleController.class).getRole(roleModel.getId(), null)).withSelfRel());

        return roleModel;
    }

    @Override
    @Nonnull
    public CollectionModel<RoleModel> toCollectionModel(Iterable<? extends RoleEntity> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .collect(Collectors.collectingAndThen(
                                Collectors.toList(),
                                roles -> CollectionModel.of(roles,
                                        linkTo(methodOn(RoleController.class).getRoles(null)).withSelfRel())
                        )
                );
    }
}
