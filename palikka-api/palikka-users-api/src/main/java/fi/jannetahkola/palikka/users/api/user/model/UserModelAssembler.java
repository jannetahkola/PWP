package fi.jannetahkola.palikka.users.api.user.model;

import fi.jannetahkola.palikka.users.api.user.UserController;
import fi.jannetahkola.palikka.users.api.user.UserRoleController;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import jakarta.annotation.Nonnull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserEntity, UserModel> {

    @Override
    @Nonnull
    public UserModel toModel(UserEntity entity) {
        UserModel userModel = UserModel.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .active(entity.getActive())
                .roles(
                        entity.getRoles().stream()
                                .map(RoleEntity::getName)
                                .collect(Collectors.toSet()))
                .build();

        userModel.add(linkTo(methodOn(UserController.class).getUser(userModel.getId())).withSelfRel());
        userModel.add(linkTo(methodOn(UserRoleController.class).getUserRoles(userModel.getId())).withRel("roles"));

        return userModel;
    }

    @Override
    @Nonnull
    public CollectionModel<UserModel> toCollectionModel(Iterable<? extends UserEntity> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .collect(Collectors.collectingAndThen(
                                Collectors.toList(),
                                users -> CollectionModel.of(users,
                                        linkTo(methodOn(UserController.class).getUsers()).withSelfRel())
                        )
                );
    }
}