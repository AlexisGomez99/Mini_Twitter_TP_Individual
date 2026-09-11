package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    static UserRepository repositoryOf(EntityManager em) {
        return new JpaUserRepository(em);
    }

    Optional<User> getForUsername(String username);

    // Buscar por id, no solo por username: el username puede cambiar en algún momento,
    // el id no. Preferible para cualquier referencia estable a un usuario.
    Optional<User> getById(Long id);

    void addUser(User user);

    // Soft delete: no borra la fila (ver User.markAsDeleted), para no romper referencias
    // como los retweets de otros usuarios sobre tweets de este.
    void deleteUser(User user);

    List<UserInfo> listUsers();
}
