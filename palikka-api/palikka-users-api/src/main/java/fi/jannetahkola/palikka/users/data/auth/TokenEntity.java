package fi.jannetahkola.palikka.users.data.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "palikka_token_blacklist")
@Getter
@Setter
public class TokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(updatable = false, nullable = false)
    private String tokenId;

    @Column(updatable = false, nullable = false)
    private LocalDateTime addedOn;
}
