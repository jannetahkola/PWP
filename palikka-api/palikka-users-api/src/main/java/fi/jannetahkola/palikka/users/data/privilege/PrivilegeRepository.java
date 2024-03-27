package fi.jannetahkola.palikka.users.data.privilege;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<PrivilegeEntity, Integer> {
    Optional<PrivilegeEntity> findByName(String name);

    List<PrivilegeEntity> findAllByDomainContainingIgnoreCaseOrNameContainingIgnoreCase(String domain, String name);
}
