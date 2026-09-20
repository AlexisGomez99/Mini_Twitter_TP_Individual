package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Tweet;

import java.util.List;
import java.util.Optional;

public interface TweetRepository {
    static TweetRepository repositoryOf(EntityManager em) {
        return new JpaTweetRepository(em);
    }

    void save(OriginalTweet tweet);

    // Devuelve el tweet exista o no haya sido borrado: hace falta poder resolver el
    // origen de un retweet aunque esté marcado como no disponible.
    Optional<Tweet> findById(Integer id);

    // Soft delete: no borra la fila (ver Tweet.markAsDeleted), para que los retweets
    // que lo tienen como origen no pierdan la referencia.
    void delete(OriginalTweet tweet);

    List<OriginalTweetInfo> findByAuthorId(Integer userId);

    List<TweetInfo> findTimelineByAuthorId(Integer userId);
}
