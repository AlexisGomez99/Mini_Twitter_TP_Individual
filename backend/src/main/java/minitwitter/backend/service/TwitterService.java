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
    private static final int TAMANIO_PAGINA_FEED = 20;
    private static final int TAMANIO_PAGINA_TWEETS = 10;

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
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
            return notary().generateTokenFor(usuario.identificador());
        });
    }

    public Integer verificarTokenYObtenerIdUsuario(String token) {
        try {
            return notary().verifyToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido", e);
        }
    }

    public Integer registrarUsuario(String nombreUsuario, String contrasenia) {
        return emf.callInTransaction(em -> {
            var usuarioOptional = UserRepository.repositorioDe(em).obtenerPorNombreUsuario(nombreUsuario);
            usuarioOptional.ifPresent(u -> {
                throw new RuntimeException("El nombre de usuario ya existe");
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

    // Caso específico donde sí hace falta buscar por username: por ejemplo, un login,
    // donde todavía no se conoce el id del usuario.
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

    public List<UserInfo> listarUsuarios() {
        return emf.callInTransaction(em -> UserRepository.repositorioDe(em).listarUsuarios());
    }

    // --- Tweets ---

    public void crearTweet(Integer idUsuario, String contenido) {
        emf.runInTransaction(em -> {
            var autor = UserRepository.repositorioDe(em).obtenerPorId(idUsuario)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idUsuario));
            TweetRepository.repositorioDe(em).guardar(new OriginalTweet(autor, contenido));
        });
    }

    // idUsuario es quien pide el borrado: solo el autor puede eliminar su tweet.
    public void eliminarTweet(Integer idUsuario, Integer idTweet) {
        emf.runInTransaction(em -> {
            var repositorio = TweetRepository.repositorioDe(em);
            repositorio.buscarPorId(idTweet)
                    .filter(tweet -> tweet instanceof OriginalTweet)
                    .map(tweet -> (OriginalTweet) tweet)
                    .ifPresent(tweet -> {
                        if (!tweet.getAuthor().getId().equals(idUsuario)) {
                            throw new ResponseStatusException(
                                    HttpStatus.FORBIDDEN, "No podés eliminar un tweet de otra persona");
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
            throw new DomainException("La página no puede ser negativa");
        }
    }

    // --- Retweets ---

    public void crearRetweet(Integer idUsuario, Integer idTweetOrigen) {
        emf.runInTransaction(em -> {
            var autor = UserRepository.repositorioDe(em).obtenerPorId(idUsuario)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idUsuario));
            // No se puede crear un retweet nuevo de un tweet que ya no está disponible,
            // aunque sí se puede seguir consultando uno ya existente (ver buscarPorId).
            var origen = TweetRepository.repositorioDe(em).buscarPorId(idTweetOrigen)
                    .filter(tweet -> !tweet.isDeleted())
                    .orElseThrow(() -> new DomainException("Tweet no encontrado: " + idTweetOrigen));
            RetweetRepository.repositorioDe(em).guardar(new Retweet(autor, origen));
        });
    }

    public void eliminarRetweet(Integer idRetweet) {
        emf.runInTransaction(em -> {
            // Se busca con TweetRepository (la clase base) porque un retweet es un Tweet;
            // TweetRepository.buscarPorId ya está pensado para devolver cualquier subtipo.
            TweetRepository.repositorioDe(em).buscarPorId(idRetweet)
                    .filter(tweet -> tweet instanceof Retweet)
                    .map(tweet -> (Retweet) tweet)
                    .ifPresent(RetweetRepository.repositorioDe(em)::eliminar);
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
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idSeguidor));
            var seguido = repositorioUsuarios.obtenerPorId(idSeguido)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idSeguido));
            var repositorioFollows = FollowRepository.repositorioDe(em);
            // Seguir a alguien que ya se sigue no hace nada (igual que dejarDeSeguir), en vez de
            // chocar contra la restricción de unicidad de la base y devolver su mensaje de error.
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
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idSeguidor));
            var seguido = repositorioUsuarios.obtenerPorId(idSeguido)
                    .filter(usuario -> !usuario.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + idSeguido));
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
