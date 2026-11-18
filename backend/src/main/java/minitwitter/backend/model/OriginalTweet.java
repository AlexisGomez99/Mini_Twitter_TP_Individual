package minitwitter.backend.model;

import minitwitter.backend.dto.OriginalTweetInfo;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ORIGINAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginalTweet extends Tweet {

    public static final int LARGO_MAXIMO_CONTENIDO = 280;
    public static final String ERROR_CONTENIDO_INVALIDO = "El tweet debe tener entre 1 y "
            + LARGO_MAXIMO_CONTENIDO + " caracteres.";

    @Column(length = LARGO_MAXIMO_CONTENIDO)
    private String content;

    public OriginalTweet(User author, String content) {
        super(author);
        validateContent(content);
        this.content = content;
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty() || content.length() > LARGO_MAXIMO_CONTENIDO) {
            throw new DomainException(ERROR_CONTENIDO_INVALIDO);
        }
    }

    @Override
    public OriginalTweetInfo toInfo() {
        String authorUsername = getAuthor().isDeleted() ? NOMBRE_CUENTA_ELIMINADA : getAuthor().getUsername();
        String content = isDeleted() ? TEXTO_PUBLICACION_NO_DISPONIBLE : getContent();
        return new OriginalTweetInfo(getId(), authorUsername, content, getCreatedAt());
    }
}
