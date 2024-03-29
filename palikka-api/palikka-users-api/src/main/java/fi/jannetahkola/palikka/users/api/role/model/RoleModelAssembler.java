package fi.jannetahkola.palikka.users.api.role.model;

import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.api.role.RoleController;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
public class RoleModelAssembler implements RepresentationModelAssembler<RoleEntity, RoleModel> {
    private final PrivilegeModelAssembler privilegeModelAssembler;

    @Override
    @Nonnull
    public RoleModel toModel(RoleEntity entity) {
        RoleModel roleModel = RoleModel.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .privileges(
                        // Sort by domain, name
                        entity.getPrivileges().stream()
                                .sorted(Comparator.comparing(PrivilegeEntity::getDomain))
                                .sorted(Comparator.comparing(PrivilegeEntity::getName))
                                .map(privilegeModelAssembler::toModel)
                                .toList())
                .build();

        roleModel.add(linkTo(methodOn(RoleController.class).getRole(roleModel.getId(), null)).withSelfRel());

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
                                        linkTo(methodOn(RoleController.class).getRoles(null))
                                                .withSelfRel(),
                                        linkTo(methodOn(RoleController.class).getRole(null, null))
                                                .withRel("role")
                                )
                        )
                );
    }
}
