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

    public static final String ERROR_ORIGEN_NULO = "El tweet de origen no puede ser nulo.";
    public static final String ERROR_RETWEET_PROPIO = "No podés hacer re-tweet de tu propio tweet.";

    @ManyToOne
    @JoinColumn(name = "origin_tweet_id")

    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tweet origin;

    public Retweet(User author, Tweet origin) {
        super(author);
        if (origin == null) {
            throw new DomainException(ERROR_ORIGEN_NULO);
        }
        if (origin.getAuthor().equals(author)) {
            throw new DomainException(ERROR_RETWEET_PROPIO);
        }
        this.origin = origin;
    }

    @Override
    public RetweetInfo toInfo() {
        String authorUsername = getAuthor().isDeleted() ? NOMBRE_CUENTA_ELIMINADA : getAuthor().getUsername();
        String originalAuthorUsername;
        String originalContent;
        if (getOrigin() instanceof OriginalTweet originalTweet) {
            var originInfo = originalTweet.toInfo();
            originalAuthorUsername = originInfo.authorUsername();
            originalContent = originInfo.content();
        } else {
            originalAuthorUsername = NOMBRE_CUENTA_ELIMINADA;
            originalContent = TEXTO_PUBLICACION_NO_DISPONIBLE;
        }
        return new RetweetInfo(
                getId(), authorUsername,
                getOrigin().getId(), originalAuthorUsername, originalContent,
                getOrigin().isDeleted(),
                getCreatedAt()
        );
    }
}
