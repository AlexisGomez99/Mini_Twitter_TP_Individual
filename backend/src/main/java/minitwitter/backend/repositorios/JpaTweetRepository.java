package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.PageResponse;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Tweet;

import java.util.Optional;

public class JpaTweetRepository implements TweetRepository {
    private final EntityManager em;

    public JpaTweetRepository(EntityManager em) {
        this.em = em;
    }

    @Override
    public void guardar(OriginalTweet tweet) {
        em.persist(tweet);
    }

    @Override
    public Optional<Tweet> buscarPorId(Integer id) {
        return Optional.ofNullable(em.find(Tweet.class, id));
    }

    @Override
    public void eliminar(OriginalTweet tweet) {
        tweet.markAsDeleted();
    }

    @Override
    public PageResponse<OriginalTweetInfo> buscarPorIdAutor(Integer idUsuario, int pagina, int tamanio) {
        var consulta = em.createQuery(
                "select o from OriginalTweet o "
                        + "where o.author.id = :idUsuario and o.deleted = false "
                        + "order by o.createdAt desc, o.id desc",
                OriginalTweet.class);
        consulta.setParameter("idUsuario", idUsuario);
        consulta.setFirstResult(desplazamientoDe(pagina, tamanio));
        consulta.setMaxResults(tamanio);
        var contenido = consulta.getResultList().stream().map(OriginalTweet::toInfo).toList();

        var conteo = em.createQuery(
                "select count(o) from OriginalTweet o "
                        + "where o.author.id = :idUsuario and o.deleted = false",
                Long.class);
        conteo.setParameter("idUsuario", idUsuario);
        return PageResponse.armar(contenido, pagina, tamanio, conteo.getSingleResult());
    }

    @Override
    public PageResponse<TweetInfo> buscarFeedPorIdSeguidor(Integer idUsuario, int pagina, int tamanio) {
        var filtroFeed = "where t.deleted = false "
                + "and (t.author.id = :idUsuario or t.author.id in ("
                + "select f.followed.id from Follow f where f.follower.id = :idUsuario)) ";
        var consulta = em.createQuery(
                "select t from Tweet t " + filtroFeed + "order by t.createdAt desc, t.id desc",
                Tweet.class);
        consulta.setParameter("idUsuario", idUsuario);
        consulta.setFirstResult(desplazamientoDe(pagina, tamanio));
        consulta.setMaxResults(tamanio);
        var contenido = consulta.getResultList().stream().map(Tweet::toInfo).toList();

        var conteo = em.createQuery("select count(t) from Tweet t " + filtroFeed, Long.class);
        conteo.setParameter("idUsuario", idUsuario);
        return PageResponse.armar(contenido, pagina, tamanio, conteo.getSingleResult());
    }

    // Una página enorme no debe desbordar el int de setFirstResult: se topea y devuelve vacío.
    private static int desplazamientoDe(int pagina, int tamanio) {
        return (int) Math.min((long) pagina * tamanio, Integer.MAX_VALUE);
    }
}
