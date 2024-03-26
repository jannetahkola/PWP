package fi.jannetahkola.palikka.users.data.privilege;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivilegeRepository extends JpaRepository<PrivilegeEntity, Integer> {
}
