package minitwitter.backend.service;

import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.*;
import minitwitter.backend.model.*;
import minitwitter.backend.repositorios.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static minitwitter.backend.model.auth.Notary.notary;

import java.util.List;
import java.util.Optional;

public class TwitterService {
    public static final int TAMANIO_PAGINA_FEED = 20;
    public static final int TAMANIO_PAGINA_TWEETS = 10;

    public static final String ERROR_CREDENCIALES_INCORRECTAS = "Usuario o contraseña incorrectos";
    public static final String ERROR_TOKEN_INVALIDO = "Token inválido";
    public static final String ERROR_USUARIO_YA_EXISTE = "El nombre de usuario ya existe";

    public static final String ERROR_USUARIO_NO_ENCONTRADO = "Usuario no encontrado: ";
    public static final String ERROR_TWEET_NO_ENCONTRADO = "Tweet no encontrado: ";
    public static final String ERROR_ELIMINAR_TWEET_AJENO = "No podés eliminar un tweet de otra persona";
    public static final String ERROR_ELIMINAR_RETWEET_AJENO = "No podés eliminar un retweet de otra persona";
    public static final String ERROR_PAGINA_NEGATIVA = "La página no puede ser negativa";

    private final EntityManagerFactory emf;

    public TwitterService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // --- AUTH ---
    public String iniciarSesion(String nombreUsuario, String contrasenia) {
        return emf.callInTransaction(em -> {
            var usuarioOptional = UserRepository.repositorioDe(em)
                    .obtenerPorNombreUsuarioYContrasenia(nombreUsuario, contrasenia);
            var usuario = usuarioOptional.orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, ERROR_CREDENCIALES_INCORRECTAS));
            return notary().generateTokenFor(usuario.identificador());
        });
    }

    public Integer verificarTokenYObtenerIdUsuario(String token) {
        try {
            return notary().verifyToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ERROR_TOKEN_INVALIDO, e);
        }
    }

    public Integer registrarUsuario(String nombreUsuario, String contrasenia) {
        return emf.callInTransaction(em -> {
            var usuarioOptional = UserRepository.repositorioDe(em).obtenerPorNombreUsuario(nombreUsuario);
            usuarioOptional.ifPresent(u -> {
                throw new RuntimeException(ERROR_USUARIO_YA_EXISTE);
            });
            var usuario = new User(nombreUsuario, contrasenia);
            em.persist(usuario);
            return usuario.identificador();
        });
    }



    // --- Usuarios ---

    public void agregarUsuario(String nombreUsuario, String contrasenia) {
        emf.runInTransaction(em -> {
            var usuario = new User(nombreUsuario, contrasenia);
            UserRepository.repositorioDe(em).agregarUsuario(usuario);
        });
    }

    public Optional<UserInfo> obtenerUsuarioPorId(Integer id) {
        return emf.callInTransaction(em ->
                UserRepository.repositorioDe(em).obtenerPorId(id)
                        .filter(usuario -> !usuario.isDeleted())
                        .map(User::toInfo)
        );
    }


    public Optional<UserInfo> obtenerUsuarioPorNombreUsuario(String nombreUsuario) {
        return emf.callInTransaction(em ->
                UserRepository.repositorioDe(em).obtenerPorNombreUsuario(nombreUsuario)
                        .filter(usuario -> !usuario.isDeleted())
                        .map(User::toInfo)
        );
    }

    public void eliminarUsuario(Integer id) {
        emf.runInTransaction(em ->
                UserRepository.repositorioDe(em).obtenerPorId(id)
                        .ifPresent(usuario -> UserRepository.repositorioDe(em).eliminarUsuario(usuario))
        );
    }

    // esto deberia estar paginado :)
    public List<UserInfo> listarUsuarios() {
        return emf.callInTransaction(em -> UserRepository.repositorioDe(em).listarUsuarios());
    }




    // --- Tweets ---

    public void crearTweet(Integer idUsuario, String contenido) {
        emf.runInTransaction(em -> {
            var autor = UserRepository.repositorioDe(em).obtenerPorId(idUsuario)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idUsuario));
            TweetRepository.repositorioDe(em).guardar(new OriginalTweet(autor, contenido));
        });
    }

    // solo el autor puede eliminar su tweet.
    public void eliminarTweet(Integer idUsuario, Integer idTweet) {
        emf.runInTransaction(em -> {
            var repositorio = TweetRepository.repositorioDe(em);
            repositorio.buscarPorId(idTweet)
                    .filter(tweet -> tweet instanceof OriginalTweet)
                    .map(tweet -> (OriginalTweet) tweet)
                    .ifPresent(tweet -> {
                        if (!tweet.getAuthor().getId().equals(idUsuario)) {
                            throw new ResponseStatusException(
                                    HttpStatus.FORBIDDEN, ERROR_ELIMINAR_TWEET_AJENO);
                        }
                        repositorio.eliminar(tweet);
                    });
        });
    }

    // Los tweets originales de un usuario
    public PageResponse<OriginalTweetInfo> listarTweetsDelUsuario(Integer idUsuario, int pagina) {
        validarPagina(pagina);
        return emf.callInTransaction(em ->
                TweetRepository.repositorioDe(em).buscarPorIdAutor(idUsuario, pagina, TAMANIO_PAGINA_TWEETS));
    }

    // El feed de inicio
    public PageResponse<TweetInfo> obtenerFeedDelUsuario(Integer idUsuario, int pagina) {
        validarPagina(pagina);
        return emf.callInTransaction(em ->
                TweetRepository.repositorioDe(em).buscarFeedPorIdSeguidor(idUsuario, pagina, TAMANIO_PAGINA_FEED));
    }

    private void validarPagina(int pagina) {
        if (pagina < 0) {
            throw new DomainException(ERROR_PAGINA_NEGATIVA);
        }
    }




    // --- Retweets ---

    public void crearRetweet(Integer idUsuario, Integer idTweetOrigen) {
        emf.runInTransaction(em -> {
            var autor = UserRepository.repositorioDe(em).obtenerPorId(idUsuario)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idUsuario));
            // No se puede crear un retweet nuevo de un tweet que ya no está disponible,
            // aunque sí se puede seguir consultando uno ya existente (ver buscarPorId).
            var origen = TweetRepository.repositorioDe(em).buscarPorId(idTweetOrigen)
                    .filter(tweet -> !tweet.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_TWEET_NO_ENCONTRADO + idTweetOrigen));
            RetweetRepository.repositorioDe(em).guardar(new Retweet(autor, origen));
        });
    }

    // idUsuario es quien pide el borrado: solo el autor puede eliminar su retweet.
    public void eliminarRetweet(Integer idUsuario, Integer idRetweet) {
        emf.runInTransaction(em -> {
            // Se busca con TweetRepository (la clase base) porque un retweet es un Tweet;
            // TweetRepository.buscarPorId ya está pensado para devolver cualquier subtipo.
            TweetRepository.repositorioDe(em).buscarPorId(idRetweet)
                    .filter(tweet -> tweet instanceof Retweet)
                    .map(tweet -> (Retweet) tweet)
                    .ifPresent(retweet -> {
                        if (!retweet.getAuthor().getId().equals(idUsuario)) {
                            throw new ResponseStatusException(
                                    HttpStatus.FORBIDDEN, ERROR_ELIMINAR_RETWEET_AJENO);
                        }
                        RetweetRepository.repositorioDe(em).eliminar(retweet);
                    });
        });
    }

    public List<RetweetInfo> listarRetweetsDelUsuario(Integer idUsuario) {
        return emf.callInTransaction(em -> RetweetRepository.repositorioDe(em).buscarPorIdAutor(idUsuario));
    }




    // --- Follows ---

    public void seguir(Integer idSeguidor, Integer idSeguido) {
        emf.runInTransaction(em -> {
            var repositorioUsuarios = UserRepository.repositorioDe(em);
            var seguidor = repositorioUsuarios.obtenerPorId(idSeguidor)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idSeguidor));
            var seguido = repositorioUsuarios.obtenerPorId(idSeguido)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idSeguido));
            var repositorioFollows = FollowRepository.repositorioDe(em);
            // un follow que ya existe choca contra unique de la bd
            if (!repositorioFollows.estaSiguiendo(idSeguidor, idSeguido)) {
                repositorioFollows.seguir(seguidor, seguido);
            }
        });
    }

    public void dejarDeSeguir(Integer idSeguidor, Integer idSeguido) {
        emf.runInTransaction(em -> {
            var repositorioUsuarios = UserRepository.repositorioDe(em);
            var seguidor = repositorioUsuarios.obtenerPorId(idSeguidor)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idSeguidor));
            var seguido = repositorioUsuarios.obtenerPorId(idSeguido)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException(ERROR_USUARIO_NO_ENCONTRADO + idSeguido));
            FollowRepository.repositorioDe(em).dejarDeSeguir(seguidor, seguido);
        });
    }

    public boolean estaSiguiendo(Integer idSeguidor, Integer idSeguido) {
        return emf.callInTransaction(em ->
                FollowRepository.repositorioDe(em).estaSiguiendo(idSeguidor, idSeguido));
    }

    public List<UserInfo> obtenerSeguidores(Integer idUsuario) {
        return emf.callInTransaction(em -> FollowRepository.repositorioDe(em).buscarSeguidores(idUsuario));
    }

    public List<UserInfo> obtenerSeguidos(Integer idUsuario) {
        return emf.callInTransaction(em -> FollowRepository.repositorioDe(em).buscarSeguidos(idUsuario));
    }
}
