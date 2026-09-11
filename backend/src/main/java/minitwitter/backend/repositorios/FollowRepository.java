package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.User;

import java.util.List;

public interface FollowRepository {
    static FollowRepository repositoryOf(EntityManager em) {
        return new JpaFollowRepository(em);
    }

    void follow(User follower, User followed);

    void unfollow(User follower, User followed);

    boolean isFollowing(Long followerId, Long followedId);

    List<UserInfo> findFollowers(Long userId);

    List<UserInfo> findFollowing(Long userId);
}
