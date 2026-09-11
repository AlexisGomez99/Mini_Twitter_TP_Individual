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
        // Un usuario no puede seguir dos veces al mismo usuario.
        @UniqueConstraint(columnNames = {"follower_id", "followed_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
            throw new DomainException("El seguidor y el seguido son obligatorios.");
        }
        if (follower.equals(followed)) {
            throw new DomainException("Un usuario no puede seguirse a sí mismo.");
        }
        this.follower = follower;
        this.followed = followed;
        this.followedAt = LocalDateTime.now();
    }
}