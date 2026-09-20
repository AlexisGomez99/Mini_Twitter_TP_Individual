package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.User;

import java.util.List;
import java.util.Optional;

public class JpaUserRepository implements UserRepository {
    private final EntityManager em;

    public JpaUserRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<User> getForUsername(String username) {
        var query = em.createQuery("from User u where u.username = :username", User.class);
        query.setParameter("username", username);
        return Optional.ofNullable(query.getSingleResultOrNull());
    }

    @Override
    public Optional<User> getById(Integer id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> fetchForUsernameAndPassword(String username, String password) {
        var exist = em.createQuery("from User u where u.username = :username and u.password = :password", User.class);
        exist.setParameter("username", username);
        exist.setParameter("password", password);
        return Optional.ofNullable(exist.getSingleResultOrNull());
    }

    @Override
    public void addUser(User user) {
        em.persist(user);
    }

    @Override
    public void deleteUser(User user) {
        user.markAsDeleted();
    }

    @Override
    public List<UserInfo> listUsers() {
        var query = em.createQuery(
                "select u from User u where u.deleted = false order by u.username", User.class);
        return query.getResultList().stream().map(User::toInfo).toList();
    }
}
