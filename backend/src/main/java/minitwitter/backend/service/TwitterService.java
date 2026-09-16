package minitwitter.backend.service;

import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.*;
import minitwitter.backend.model.*;
import minitwitter.backend.repositorios.*;
import static minitwitter.backend.model.auth.Notary.notary;

import java.util.List;
import java.util.Optional;

public class TwitterService {
    private final EntityManagerFactory emf;

    public TwitterService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // --- AUTH ---
    public String login(String username, String password) {
        return emf.callInTransaction(em -> {
            var usuarioOptional = UserRepository.repositoryOf(em).fetchForUsernameAndPassword(username, password);
            var usuario = usuarioOptional.orElseThrow(() -> new RuntimeException("Usuario o contraseña incorrectos"));
            return notary().generateTokenFor(usuario.identificador());
        });
    }

    public Integer verificarTokenAndGetIdUsuario(String token) {
        try {
            return notary().verifyToken(token);
        } catch (Exception e) {
            throw new RuntimeException("Token inválido", e);
        }
    }

    public Integer registrarUsuario(String username, String password) {
        return emf.callInTransaction(em -> {
            var usuarioOptional = UserRepository.repositoryOf(em).getForUsername(username);
            usuarioOptional.ifPresent(u -> {
                throw new RuntimeException("El nombre de usuario ya existe");
            });
            var usuario = new User(username, password);
            em.persist(usuario);
            return usuario.identificador();
        });
    }
    // --- Usuarios ---

    public void addUser(String username, String password) {
        emf.runInTransaction(em -> {
            var user = new User(username, password);
            UserRepository.repositoryOf(em).addUser(user);
        });
    }

    public Optional<UserInfo> getUserById(Integer id) {
        return emf.callInTransaction(em ->
                UserRepository.repositoryOf(em).getById(id)
                        .filter(user -> !user.isDeleted())
                        .map(User::toInfo)
        );
    }

    // Caso específico donde sí hace falta buscar por username: por ejemplo, un login,
    // donde todavía no se conoce el id del usuario.
    public Optional<UserInfo> getUserByUsername(String username) {
        return emf.callInTransaction(em ->
                UserRepository.repositoryOf(em).getForUsername(username)
                        .filter(user -> !user.isDeleted())
                        .map(User::toInfo)
        );
    }

    public void deleteUser(Integer id) {
        emf.runInTransaction(em ->
                UserRepository.repositoryOf(em).getById(id)
                        .ifPresent(user -> UserRepository.repositoryOf(em).deleteUser(user))
        );
    }

    public List<UserInfo> listUsers() {
        return emf.callInTransaction(em -> UserRepository.repositoryOf(em).listUsers());
    }

    // --- Tweets ---

    public void createTweet(Integer userId, String content) {
        emf.runInTransaction(em -> {
            var author = UserRepository.repositoryOf(em).getById(userId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + userId));
            TweetRepository.repositoryOf(em).save(new OriginalTweet(author, content));
        });
    }

    public void deleteTweet(Integer tweetId) {
        emf.runInTransaction(em -> {
            var repository = TweetRepository.repositoryOf(em);
            repository.findById(tweetId)
                    .filter(tweet -> tweet instanceof OriginalTweet)
                    .map(tweet -> (OriginalTweet) tweet)
                    .ifPresent(repository::delete);
        });
    }

    public List<OriginalTweetInfo> listTweetsForUserId(Integer userId) {
        return emf.callInTransaction(em -> TweetRepository.repositoryOf(em).findByAuthorId(userId));
    }

    public List<TweetInfo> getTimelineForUserId(Integer userId) {
        return emf.callInTransaction(em -> TweetRepository.repositoryOf(em).findTimelineByAuthorId(userId));
    }

    // --- Retweets ---

    public void createRetweet(Integer userId, Integer originTweetId) {
        emf.runInTransaction(em -> {
            var author = UserRepository.repositoryOf(em).getById(userId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + userId));
            // No se puede crear un retweet nuevo de un tweet que ya no está disponible,
            // aunque sí se puede seguir consultando uno ya existente (ver findById).
            var origin = TweetRepository.repositoryOf(em).findById(originTweetId)
                    .filter(tweet -> !tweet.isDeleted())
                    .orElseThrow(() -> new DomainException("Tweet no encontrado: " + originTweetId));
            RetweetRepository.repositoryOf(em).save(new Retweet(author, origin));
        });
    }

    public void deleteRetweet(Integer retweetId) {
        emf.runInTransaction(em -> {
            // Se busca con TweetRepository (la clase base) porque un retweet es un Tweet;
            // TweetRepository.findById ya está pensado para devolver cualquier subtipo.
            TweetRepository.repositoryOf(em).findById(retweetId)
                    .filter(tweet -> tweet instanceof Retweet)
                    .map(tweet -> (Retweet) tweet)
                    .ifPresent(RetweetRepository.repositoryOf(em)::delete);
        });
    }

    public List<RetweetInfo> listRetweetsForUserId(Integer userId) {
        return emf.callInTransaction(em -> RetweetRepository.repositoryOf(em).findByAuthorId(userId));
    }

    // --- Follows ---

    public void follow(Integer followerId, Integer followedId) {
        emf.runInTransaction(em -> {
            var userRepository = UserRepository.repositoryOf(em);
            var follower = userRepository.getById(followerId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + followerId));
            var followed = userRepository.getById(followedId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + followedId));
            FollowRepository.repositoryOf(em).follow(follower, followed);
        });
    }

    public void unfollow(Integer followerId, Integer followedId) {
        emf.runInTransaction(em -> {
            var userRepository = UserRepository.repositoryOf(em);
            var follower = userRepository.getById(followerId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + followerId));
            var followed = userRepository.getById(followedId)
                    .filter(user -> !user.isDeleted())
                    .orElseThrow(() -> new DomainException("Usuario no encontrado: " + followedId));
            FollowRepository.repositoryOf(em).unfollow(follower, followed);
        });
    }

    public boolean isFollowing(Integer followerId, Integer followedId) {
        return emf.callInTransaction(em ->
                FollowRepository.repositoryOf(em).isFollowing(followerId, followedId));
    }

    public List<UserInfo> getFollowers(Integer userId) {
        return emf.callInTransaction(em -> FollowRepository.repositoryOf(em).findFollowers(userId));
    }

    public List<UserInfo> getFollowing(Integer userId) {
        return emf.callInTransaction(em -> FollowRepository.repositoryOf(em).findFollowing(userId));
    }
}
