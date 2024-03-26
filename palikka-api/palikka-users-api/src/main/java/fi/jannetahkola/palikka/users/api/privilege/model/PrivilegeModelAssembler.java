package fi.jannetahkola.palikka.users.api.privilege.model;

import fi.jannetahkola.palikka.users.api.privilege.PrivilegeController;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import jakarta.annotation.Nonnull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PrivilegeModelAssembler implements RepresentationModelAssembler<PrivilegeEntity, PrivilegeModel> {
    @Override
    @Nonnull
    public PrivilegeModel toModel(PrivilegeEntity entity) {
        return PrivilegeModel.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .name(entity.getName())
                .build();
    }

    @Override
    @Nonnull
    public CollectionModel<PrivilegeModel> toCollectionModel(Iterable<? extends PrivilegeEntity> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        roles -> CollectionModel.of(roles,
                                linkTo(methodOn(PrivilegeController.class).getPrivileges(null)).withSelfRel())
                        )
                );
    }
}
