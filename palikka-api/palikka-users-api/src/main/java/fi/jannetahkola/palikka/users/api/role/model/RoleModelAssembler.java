package fi.jannetahkola.palikka.users.api.role.model;

import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.api.role.RoleController;
import fi.jannetahkola.palikka.users.api.role.RolePrivilegeController;
import fi.jannetahkola.palikka.users.api.user.UserRoleController;
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

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
        roleModel.add(linkTo(methodOn(RolePrivilegeController.class).getRolePrivileges(roleModel.getId(), null)).withRel("privileges"));

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

    /**
     * Converts a role entity into a model with links that refer to the role as a user role. This is in contrast to
     * the default links added by {@link RoleModelAssembler#toModel(RoleEntity)}, which refer to the role as an
     * isolated resource.
     * @param entity Entity to convert
     * @param userId Target user id
     * @return Role model
     */
    public RoleModel toModel(RoleEntity entity, Integer userId) {
        RoleModel model = toModel(entity);
        model.removeLinks(); // Remove the default links added by the assembler
        return model.add(linkTo(methodOn(UserRoleController.class).getUserRole(userId, model.getId())).withSelfRel()
                .andAffordance(afford(methodOn(UserRoleController.class).deleteUserRoles(userId, model.getId()))))
                .add(linkTo(methodOn(RolePrivilegeController.class).getRolePrivileges(model.getId(), null)).withRel("privileges"));
    }
}
