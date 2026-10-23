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
    public Optional<User> obtenerPorNombreUsuario(String nombreUsuario) {
        var consulta = em.createQuery("from User u where u.username = :nombreUsuario", User.class);
        consulta.setParameter("nombreUsuario", nombreUsuario);
        return Optional.ofNullable(consulta.getSingleResultOrNull());
    }

    @Override
    public Optional<User> obtenerPorId(Integer id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> obtenerPorNombreUsuarioYContrasenia(String nombreUsuario, String contrasenia) {
        var consulta = em.createQuery(
                "from User u where u.username = :nombreUsuario and u.password = :contrasenia", User.class);
        consulta.setParameter("nombreUsuario", nombreUsuario);
        consulta.setParameter("contrasenia", contrasenia);
        return Optional.ofNullable(consulta.getSingleResultOrNull());
    }

    @Override
    public void agregarUsuario(User usuario) {
        em.persist(usuario);
    }

    @Override
    public void eliminarUsuario(User usuario) {
        usuario.markAsDeleted();
    }

    @Override
    public List<UserInfo> listarUsuarios() {
        var consulta = em.createQuery(
                "select u from User u where u.deleted = false order by u.username", User.class);
        return consulta.getResultList().stream().map(User::toInfo).toList();
    }
}
