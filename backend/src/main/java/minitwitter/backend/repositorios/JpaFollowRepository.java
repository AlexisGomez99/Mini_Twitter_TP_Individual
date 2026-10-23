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
    public void seguir(User seguidor, User seguido) {
        em.persist(new Follow(seguidor, seguido));
    }

    @Override
    public void dejarDeSeguir(User seguidor, User seguido) {
        var consulta = em.createQuery(
                "delete from Follow f where f.follower = :seguidor and f.followed = :seguido");
        consulta.setParameter("seguidor", seguidor);
        consulta.setParameter("seguido", seguido);
        consulta.executeUpdate();
    }

    @Override
    public boolean estaSiguiendo(Integer idSeguidor, Integer idSeguido) {
        var consulta = em.createQuery(
                "select count(f) from Follow f "
                        + "where f.follower.id = :idSeguidor "
                        + "and f.followed.id = :idSeguido",
                Long.class);
        consulta.setParameter("idSeguidor", idSeguidor);
        consulta.setParameter("idSeguido", idSeguido);
        return consulta.getSingleResult() > 0;
    }

    @Override
    public List<UserInfo> buscarSeguidores(Integer idUsuario) {
        // No hace falta filtrar cuentas eliminadas acá: User.markAsDeleted() ya borra
        // todas las relaciones de follow del usuario (en ambos sentidos) al eliminarlo.
        var consulta = em.createQuery(
                "select f.follower from Follow f where f.followed.id = :idUsuario",
                User.class);
        consulta.setParameter("idUsuario", idUsuario);
        return consulta.getResultList().stream().map(User::toInfo).toList();
    }

    @Override
    public List<UserInfo> buscarSeguidos(Integer idUsuario) {
        var consulta = em.createQuery(
                "select f.followed from Follow f where f.follower.id = :idUsuario",
                User.class);
        consulta.setParameter("idUsuario", idUsuario);
        return consulta.getResultList().stream().map(User::toInfo).toList();
    }
}
