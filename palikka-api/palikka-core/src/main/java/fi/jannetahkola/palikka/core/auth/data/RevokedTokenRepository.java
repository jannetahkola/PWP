package fi.jannetahkola.palikka.core.auth.data;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(RedisTemplate.class)
public interface RevokedTokenRepository extends CrudRepository<RevokedTokenEntity, String> {
}
