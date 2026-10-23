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
    public void guardar(Retweet retweet) {
        em.persist(retweet);
    }

    @Override
    public void eliminar(Retweet retweet) {
        retweet.markAsDeleted();
    }

    @Override
    public List<RetweetInfo> buscarPorIdAutor(Integer idUsuario) {
        var consulta = em.createQuery(
                "select r from Retweet r "
                        + "where r.author.id = :idUsuario and r.deleted = false "
                        + "order by r.createdAt desc",
                Retweet.class);
        consulta.setParameter("idUsuario", idUsuario);
        return consulta.getResultList().stream().map(Retweet::toInfo).toList();
    }
}
