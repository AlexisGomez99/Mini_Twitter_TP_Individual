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

    @Column(length = 280)
    private String content;

    public OriginalTweet(User author, String content) {
        super(author);
        validateContent(content);
        this.content = content;
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty() || content.length() > 280) {
            throw new DomainException("El tweet debe tener entre 1 y 280 caracteres.");
        }
    }

    // Además de marcar el tweet como borrado, se descarta el contenido: no tiene sentido
    // seguir guardando el texto de una publicación que ya no se va a mostrar.
    @Override
    public void markAsDeleted() {
        super.markAsDeleted();
        this.content = null;
    }

    @Override
    public OriginalTweetInfo toInfo() {
        String authorUsername = getAuthor().isDeleted() ? "Cuenta eliminada" : getAuthor().getUsername();
        String content = isDeleted() ? "Publicación no disponible" : getContent();
        return new OriginalTweetInfo(getId(), authorUsername, content, getCreatedAt());
    }
}
