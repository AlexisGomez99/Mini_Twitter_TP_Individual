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

    public static final String ERROR_AUTOR_OBLIGATORIO = "El tweet debe tener un autor.";
    public static final String TEXTO_PUBLICACION_NO_DISPONIBLE = "Publicación no disponible";
    public static final String NOMBRE_CUENTA_ELIMINADA = "Cuenta eliminada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // Setter público solo acá
    @Setter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Soft delete
    @Column(nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "origin")
    private List<Retweet> retweets = new ArrayList<>();

    protected Tweet(User author) {
        if (author == null) {
            throw new DomainException(ERROR_AUTOR_OBLIGATORIO);
        }
        this.author = author;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public abstract TweetInfo toInfo();
}
