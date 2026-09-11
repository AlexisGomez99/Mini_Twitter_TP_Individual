package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.model.Retweet;

import java.util.List;

public interface RetweetRepository {
    static RetweetRepository repositoryOf(EntityManager em) {
        return new JpaRetweetRepository(em);
    }

    void save(Retweet retweet);

    // Soft delete: no borra la fila (ver Tweet.markAsDeleted).
    void delete(Retweet retweet);

    List<RetweetInfo> findByAuthorId(Long userId);
}
