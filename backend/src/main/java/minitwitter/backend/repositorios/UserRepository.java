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

    Optional<User> obtenerPorId(Integer id);

    Optional<User> obtenerPorNombreUsuarioYContrasenia(String nombreUsuario, String contrasenia);

    void agregarUsuario(User usuario);

    void eliminarUsuario(User usuario);

    List<UserInfo> listarUsuarios();
}
