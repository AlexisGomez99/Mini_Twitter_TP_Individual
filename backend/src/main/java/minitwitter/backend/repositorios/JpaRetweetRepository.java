package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.model.Retweet;

import java.util.List;

public class JpaRetweetRepository implements RetweetRepository {
    private final EntityManager em;

    public JpaRetweetRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(Retweet retweet) {
        em.persist(retweet);
    }

    @Override
    public void delete(Retweet retweet) {
        retweet.markAsDeleted();
    }

    @Override
    public List<RetweetInfo> findByAuthorId(Integer userId) {
        var query = em.createQuery(
                "select r from Retweet r "
                        + "where r.author.id = :userId and r.deleted = false "
                        + "order by r.createdAt desc",
                Retweet.class);
        query.setParameter("userId", userId);
        return query.getResultList().stream().map(Retweet::toInfo).toList();
    }
}
