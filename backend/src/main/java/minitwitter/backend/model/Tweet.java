package minitwitter.backend.model;

import minitwitter.backend.dto.TweetInfo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tweets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tweet_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Tweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // Setter público solo acá: hace falta en los tests para fijar la fecha de creación
    // y así garantizar el orden del timeline sin depender de LocalDateTime.now().
    @Setter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Soft delete: se conserva la fila (sin cascade/orphanRemoval acá) para que un
    // retweet de otro usuario que tenga a este tweet como origen no pierda su referencia.
    @Column(nullable = false)
    private boolean deleted;

    // Retweets que tienen a este tweet como origen. Sin cascade: borrar (soft-delete)
    // este tweet no debe tocar para nada los retweets que lo referencian, para que
    // puedan seguir mostrándose (con el origen marcado como no disponible).
    @OneToMany(mappedBy = "origin")
    private List<Retweet> retweets = new ArrayList<>();

    protected Tweet(User author) {
        if (author == null) {
            throw new DomainException("El tweet debe tener un autor.");
        }
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public abstract TweetInfo toInfo();
}
