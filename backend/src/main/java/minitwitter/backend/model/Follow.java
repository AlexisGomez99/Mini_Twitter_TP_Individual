package minitwitter.backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"follower_id", "followed_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    public static final String ERROR_PARTICIPANTES_OBLIGATORIOS = "El seguidor y el seguido son obligatorios.";
    public static final String ERROR_SEGUIRSE_A_SI_MISMO = "Un usuario no puede seguirse a sí mismo.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower; // Seguidor

    @ManyToOne(optional = false)
    @JoinColumn(name = "followed_id", nullable = false)
    private User followed; // Seguido

    @Column(nullable = false)
    private LocalDateTime followedAt;

    public Follow(User follower, User followed) {
        if (follower == null || followed == null) {
            throw new DomainException(ERROR_PARTICIPANTES_OBLIGATORIOS);
        }
        if (follower.equals(followed)) {
            throw new DomainException(ERROR_SEGUIRSE_A_SI_MISMO);
        }
        this.follower = follower;
        this.followed = followed;
        this.followedAt = LocalDateTime.now();
    }
}