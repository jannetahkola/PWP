package fi.jannetahkola.palikka.users.data.privilege;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<PrivilegeEntity, Integer> {
    Optional<PrivilegeEntity> findByName(String name);

    List<PrivilegeEntity> findAllByDomainContainingIgnoreCaseOrNameContainingIgnoreCase(String domain, String name);

    @Query(
            value = "select * from palikka_privilege p " +
                    "where p.id in (" +
                        "select rp.privilege_id " +
                        "from palikka_role_privilege rp " +
                        "where rp.role_id = :roleId" +
                    ")",
            nativeQuery = true)
    List<PrivilegeEntity> findAllByRoleId(int roleId);
}
