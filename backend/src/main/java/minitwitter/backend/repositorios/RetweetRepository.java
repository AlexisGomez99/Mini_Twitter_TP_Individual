package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.model.Retweet;

import java.util.List;

public interface RetweetRepository {
    static RetweetRepository repositorioDe(EntityManager em) {
        return new JpaRetweetRepository(em);
    }

    void guardar(Retweet retweet);

    // Soft delete: no borra la fila.
    void eliminar(Retweet retweet);

    List<RetweetInfo> buscarPorIdAutor(Integer idUsuario);
}
