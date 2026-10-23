package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    static UserRepository repositorioDe(EntityManager em) {
        return new JpaUserRepository(em);
    }

    Optional<User> obtenerPorNombreUsuario(String nombreUsuario);

    // Buscar por id, no solo por username: el username puede cambiar en algún momento,
    // el id no. Preferible para cualquier referencia estable a un usuario.
    Optional<User> obtenerPorId(Integer id);

    Optional<User> obtenerPorNombreUsuarioYContrasenia(String nombreUsuario, String contrasenia);

    void agregarUsuario(User usuario);

    // Soft delete: no borra la fila (ver User.markAsDeleted), para no romper referencias
    // como los retweets de otros usuarios sobre tweets de este.
    void eliminarUsuario(User usuario);

    List<UserInfo> listarUsuarios();
}
