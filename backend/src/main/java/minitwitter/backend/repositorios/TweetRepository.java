package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.PageResponse;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Tweet;

import java.util.Optional;

public interface TweetRepository {
    static TweetRepository repositorioDe(EntityManager em) {
        return new JpaTweetRepository(em);
    }

    void guardar(OriginalTweet tweet);

    Optional<Tweet> buscarPorId(Integer id);

    void eliminar(OriginalTweet tweet);

    PageResponse<OriginalTweetInfo> buscarPorIdAutor(Integer idUsuario, int pagina, int tamanio);

    // Tweets y retweets de idUsuario y de los usuarios que sigue, del más nuevo al más viejo
    PageResponse<TweetInfo> buscarFeedPorIdSeguidor(Integer idUsuario, int pagina, int tamanio);
}
