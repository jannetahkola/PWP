package fi.jannetahkola.palikka.users.data.privilege;

import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "palikka_privilege",
        uniqueConstraints = @UniqueConstraint(
                name = "idx_unique_domain_name",
                columnNames = {"domain", "name"}))
@Getter
@Setter
public class PrivilegeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private String name;

    private String domainDescription;

    @Setter(AccessLevel.NONE)
    @ManyToMany(mappedBy = "privileges")
    private Set<RoleEntity> roles = new HashSet<>();

    public String getAsAuthority() {
        return this.getDomain() + "_" + this.getName();
    }

    public void addRole(RoleEntity role) {
        this.roles.add(role);
        role.getPrivileges().add(this);
    }

    public void removeRole(RoleEntity role) {
        this.roles.remove(role);
        role.getPrivileges().remove(this);
    }

    @PreRemove
    private void removeAssociations() {
        for (RoleEntity role : this.roles) {
            role.getPrivileges().remove(this);
        }
    }
}
