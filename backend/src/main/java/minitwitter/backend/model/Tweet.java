package minitwitter.backend.model;

import minitwitter.backend.exception.DomainException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tweets")
@Getter
@Setter
@NoArgsConstructor
public class Tweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 280)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "origin_tweet_id")
    private Tweet origin;

    // Tweet Original
    public Tweet(User author, String content) {
        if (author == null) {
            throw new DomainException("El tweet debe tener un autor.");
        }
        validateContent(content);
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Re-tweet
    public Tweet(User author, Tweet origin) {
        if (author == null) {
            throw new DomainException("El retweet debe tener un autor.");
        }
        if (origin == null) {
            throw new DomainException("El tweet de origen no puede ser nulo.");
        }
        if (origin.getAuthor().equals(author)) {
            throw new DomainException("No podés hacer re-tweet de tu propio tweet.");
        }

        this.author = author;
        this.origin = origin;
        this.content = null; // re-tweet no tiene texto adicional
        this.createdAt = LocalDateTime.now();
    }

    public boolean isRetweet() {
        return this.origin != null;
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty() || content.length() > 280) {
            throw new DomainException("El tweet debe tener entre 1 y 280 caracteres.");
        }
    }
}