package fi.jannetahkola.palikka.users.data.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    boolean existsByUsername(String username);

    @Query(value = "select case when (count(*) > 0) then true else false end from palikka_user pu where pu.username = :username and pu.username != :except", nativeQuery = true)
    boolean existsByUsernameExcept(String username, String except);

    Optional<UserEntity> findByUsername(String username);
}
