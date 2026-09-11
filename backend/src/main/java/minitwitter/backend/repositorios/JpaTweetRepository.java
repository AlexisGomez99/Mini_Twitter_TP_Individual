package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Tweet;

import java.util.List;
import java.util.Optional;

public class JpaTweetRepository implements TweetRepository {
    private final EntityManager em;

    public JpaTweetRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void save(OriginalTweet tweet) {
        em.persist(tweet);
    }

    @Override
    public Optional<Tweet> findById(Long id) {
        return Optional.ofNullable(em.find(Tweet.class, id));
    }

    @Override
    public void delete(OriginalTweet tweet) {
        tweet.markAsDeleted();
    }

    @Override
    public List<OriginalTweetInfo> findByAuthorId(Long userId) {
        var query = em.createQuery(
                "select o from OriginalTweet o "
                        + "where o.author.id = :userId and o.deleted = false "
                        + "order by o.createdAt desc",
                OriginalTweet.class);
        query.setParameter("userId", userId);
        return query.getResultList().stream().map(OriginalTweet::toInfo).toList();
    }

    @Override
    public List<TweetInfo> findTimelineByAuthorId(Long userId) {
        var query = em.createQuery(
                "select t from Tweet t "
                        + "where t.author.id = :userId and t.deleted = false "
                        + "order by t.createdAt desc",
                Tweet.class);
        query.setParameter("userId", userId);
        return query.getResultList().stream().map(Tweet::toInfo).toList();
    }
}
