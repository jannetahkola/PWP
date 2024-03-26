package fi.jannetahkola.palikka.users.data.role;

import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "palikka_role")
@Getter
@Setter
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Setter(AccessLevel.NONE)
    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();

    @Setter(AccessLevel.NONE)
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "palikka_role_privilege",
            joinColumns = @JoinColumn(name="role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "privilege_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "privilege_id"})
    )
    private Set<PrivilegeEntity> privileges = new HashSet<>();

    public void addUser(UserEntity user) {
        users.add(user);
        user.getRoles().add(this);
    }

    public void removeUser(UserEntity user) {
        users.remove(user);
        user.getRoles().remove(this);
    }

    public void addPrivilege(PrivilegeEntity privilege) {
        this.privileges.add(privilege);
        privilege.getRoles().add(this);
    }

    public void removePrivilege(PrivilegeEntity privilege) {
        this.privileges.remove(privilege);
        privilege.getRoles().remove(this);
    }

    @PreRemove
    private void removeAssociations() {
        for (UserEntity user : this.users) {
            user.getRoles().remove(this);
        }
        for (PrivilegeEntity privilege : this.privileges) {
            privilege.getRoles().remove(this);
        }
    }
}
