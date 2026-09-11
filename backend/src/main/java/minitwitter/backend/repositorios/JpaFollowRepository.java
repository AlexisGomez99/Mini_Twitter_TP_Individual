package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.Follow;
import minitwitter.backend.model.User;

import java.util.List;

public class JpaFollowRepository implements FollowRepository {
    private final EntityManager em;

    public JpaFollowRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void follow(User follower, User followed) {
        em.persist(new Follow(follower, followed));
    }

    @Override
    public void unfollow(User follower, User followed) {
        var query = em.createQuery(
                "delete from Follow f where f.follower = :follower and f.followed = :followed");
        query.setParameter("follower", follower);
        query.setParameter("followed", followed);
        query.executeUpdate();
    }

    @Override
    public boolean isFollowing(Long followerId, Long followedId) {
        var query = em.createQuery(
                "select count(f) from Follow f "
                        + "where f.follower.id = :followerId "
                        + "and f.followed.id = :followedId",
                Long.class);
        query.setParameter("followerId", followerId);
        query.setParameter("followedId", followedId);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<UserInfo> findFollowers(Long userId) {
        // No hace falta filtrar cuentas eliminadas acá: User.markAsDeleted() ya borra
        // todas las relaciones de follow del usuario (en ambos sentidos) al eliminarlo.
        var query = em.createQuery(
                "select f.follower from Follow f where f.followed.id = :userId",
                User.class);
        query.setParameter("userId", userId);
        return query.getResultList().stream().map(User::toInfo).toList();
    }

    @Override
    public List<UserInfo> findFollowing(Long userId) {
        var query = em.createQuery(
                "select f.followed from Follow f where f.follower.id = :userId",
                User.class);
        query.setParameter("userId", userId);
        return query.getResultList().stream().map(User::toInfo).toList();
    }
}
