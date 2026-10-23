package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.User;

import java.util.List;

public interface FollowRepository {
    static FollowRepository repositorioDe(EntityManager em) {
        return new JpaFollowRepository(em);
    }

    void seguir(User seguidor, User seguido);

    void dejarDeSeguir(User seguidor, User seguido);

    boolean estaSiguiendo(Integer idSeguidor, Integer idSeguido);

    List<UserInfo> buscarSeguidores(Integer idUsuario);

    List<UserInfo> buscarSeguidos(Integer idUsuario);
}
