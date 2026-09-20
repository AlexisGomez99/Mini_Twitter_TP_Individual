package minitwitter.backend.model;

import minitwitter.backend.dto.RetweetInfo;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@DiscriminatorValue("RETWEET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Retweet extends Tweet {

    @ManyToOne
    @JoinColumn(name = "origin_tweet_id")
    // El flujo normal de la app ya no borra tweets físicamente (ver Tweet.markAsDeleted):
    // el origen se conserva siempre, marcado como no disponible en vez de eliminado.
    // Este ON DELETE CASCADE es solo un respaldo a nivel de esquema, por si algún borrado
    // llegara a la base sin pasar por JPA (SQL directo, otra app, una migración manual).
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tweet origin;

    public Retweet(User author, Tweet origin) {
        super(author);
        if (origin == null) {
            throw new DomainException("El tweet de origen no puede ser nulo.");
        }
        if (origin.getAuthor().equals(author)) {
            throw new DomainException("No podés hacer re-tweet de tu propio tweet.");
        }
        this.origin = origin;
    }

    @Override
    public RetweetInfo toInfo() {
        String authorUsername = getAuthor().isDeleted() ? "Cuenta eliminada" : getAuthor().getUsername();
        String originalAuthorUsername;
        String originalContent;
        // Se reutiliza el toInfo() del origen para no duplicar la lógica de "no disponible"
        // (tweet borrado y/o cuenta eliminada). Si el origen no es un OriginalTweet (por
        // ejemplo, ya fue borrado y reemplazado por otro tipo en el futuro) no hay
        // contenido propio que mostrar.
        if (getOrigin() instanceof OriginalTweet originalTweet) {
            var originInfo = originalTweet.toInfo();
            originalAuthorUsername = originInfo.authorUsername();
            originalContent = originInfo.content();
        } else {
            originalAuthorUsername = "Cuenta eliminada";
            originalContent = "Publicación no disponible";
        }
        return new RetweetInfo(
                getId(), authorUsername,
                getOrigin().getId(), originalAuthorUsername, originalContent,
                getCreatedAt()
        );
    }
}
