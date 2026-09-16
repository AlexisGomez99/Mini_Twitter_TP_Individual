package minitwitter.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwitterServiceTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private TwitterService twitterService;

    private User agomez;
    private User jperez;
    private OriginalTweet jperezTweet;
    private Integer agomezId;
    private Integer jperezId;

    @BeforeAll
    static void beforeEverything() {
        emf = new EmfBuilder().h2Memory().withDropAndCreateDDL().build();
    }

    @BeforeEach
    void beforeEach() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        twitterService = new TwitterService(emf);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");
        jperezTweet = new OriginalTweet(jperez, "Tweet de Luis");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(jperezTweet);
        em.getTransaction().commit();

        agomezId = agomez.getId();
        jperezId = jperez.getId();
    }

    // --- Auth ---

    @Test
    void shouldLoginWithValidCredentialsAndReturnATokenForTheUser() {
        // Set up (agomez cargado en beforeEach con password "password123")

        // Desarrollo
        String token = twitterService.login("agomez", "password123");

        // Evaluación: el token generado corresponde al id real de agomez
        assertThat(twitterService.verificarTokenAndGetIdUsuario(token)).isEqualTo(agomezId);
    }

    @Test
    void shouldFailToLoginWithAnIncorrectPassword() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.login("agomez", "password_incorrecta"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldFailToLoginWithANonexistentUsername() {
        // Set up (usuarios cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.login("no_existe", "cualquier_clave"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldFailToVerifyAnInvalidToken() {
        // Set up
        String invalidToken = "esto-no-es-un-jwt-valido";

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.verificarTokenAndGetIdUsuario(invalidToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRegisterANewUserAndReturnItsId() {
        // Set up (agomez y jperez ya existen; se registra uno distinto)

        // Desarrollo
        Integer newUserId = twitterService.registrarUsuario("nuevo_user", "clave12345");

        // Evaluación
        assertThat(newUserId).isEqualTo(twitterService.getUserByUsername("nuevo_user").orElseThrow().id());
    }

    @Test
    void shouldFailToRegisterAUsernameThatAlreadyExists() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.registrarUsuario("agomez", "otra_clave"))
                .isInstanceOf(RuntimeException.class);
    }

    // --- Usuarios ---

    @Test
    void shouldAddANewUser() {
        // Set up (agomez y jperez ya existen; se agrega uno distinto)

        // Desarrollo
        twitterService.addUser("nuevo_user", "clave12345");

        // Evaluación
        assertThat(twitterService.getUserByUsername("nuevo_user")).isPresent();
    }

    @Test
    void shouldFindAnExistingUserByUsername() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        var found = twitterService.getUserByUsername("agomez");

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnEmptyWhenUsernameDoesNotExist() {
        // Set up (usuarios cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo
        var found = twitterService.getUserByUsername("no_existe");

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAnExistingUserById() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        var found = twitterService.getUserById(agomezId);

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get().username()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnEmptyWhenIdDoesNotExist() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo
        var found = twitterService.getUserById(999999);

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldDeleteAnExistingUser() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo
        twitterService.deleteUser(agomezId);

        // Evaluación
        assertThat(twitterService.getUserById(agomezId)).isEmpty();
    }

    @Test
    void shouldListAllUsers() {
        // Set up (agomez y jperez cargados en beforeEach)

        // Desarrollo
        var users = twitterService.listUsers();

        // Evaluación
        assertThat(users).hasSize(2);
        assertThat(users).extracting("username").containsExactlyInAnyOrder("agomez", "jperez");
    }

    @Test
    void shouldDoNothingWhenDeletingANonexistentUser() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> twitterService.deleteUser(999999)).doesNotThrowAnyException();
    }

    // --- Tweets ---

    @Test
    void shouldCreateATweetForAnExistingUser() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        twitterService.createTweet(agomezId, "Mi primer tweet");

        // Evaluación
        var tweets = twitterService.listTweetsForUserId(agomezId);
        assertThat(tweets).hasSize(1);
        assertThat(tweets.get(0).content()).isEqualTo("Mi primer tweet");
    }

    @Test
    void shouldFailToCreateATweetForANonexistentUser() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createTweet(999999, "contenido"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToCreateATweetForADeletedUser() {
        // Set up
        twitterService.deleteUser(agomezId);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createTweet(agomezId, "contenido"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldDeleteAnExistingTweet() {
        // Set up
        twitterService.createTweet(agomezId, "Tweet a borrar");
        Integer tweetId = twitterService.listTweetsForUserId(agomezId).get(0).id();

        // Desarrollo
        twitterService.deleteTweet(tweetId);

        // Evaluación
        assertThat(twitterService.listTweetsForUserId(agomezId)).isEmpty();
    }

    @Test
    void shouldDoNothingWhenDeletingANonexistentTweet() {
        // Set up (base sin ningún tweet con id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> twitterService.deleteTweet(999999)).doesNotThrowAnyException();
    }

    @Test
    void shouldListOnlyOriginalTweetsForAUser() {
        // Set up
        twitterService.createTweet(agomezId, "Tweet de Ana");

        // Desarrollo
        var tweets = twitterService.listTweetsForUserId(agomezId);

        // Evaluación
        assertThat(tweets).hasSize(1);
        assertThat(tweets.get(0).authorUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnTheTimelineOrderedByDateDescending() {
        // Set up: agomez tiene un tweet propio y retweetea el de jperez, con fechas fijas
        // para garantizar el orden esperado del timeline.
        OriginalTweet agomezTweet = new OriginalTweet(agomez, "Tweet de Ana");
        agomezTweet.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        Retweet agomezRetweet = new Retweet(agomez, jperezTweet);
        agomezRetweet.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        em.getTransaction().begin();
        em.persist(agomezTweet);
        em.persist(agomezRetweet);
        em.getTransaction().commit();

        // Desarrollo
        var timeline = twitterService.getTimelineForUserId(agomezId);

        // Evaluación: el retweet (12:00) es más nuevo que el tweet propio (10:00)
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).createdAt()).isAfter(timeline.get(1).createdAt());
    }

    // --- Retweets ---

    @Test
    void shouldCreateARetweetForAnExistingUserAndTweet() {
        // Set up (usuarios y tweet cargados en beforeEach)

        // Desarrollo
        twitterService.createRetweet(agomezId, jperezTweet.getId());

        // Evaluación
        var retweets = twitterService.listRetweetsForUserId(agomezId);
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void shouldFailToCreateARetweetForANonexistentUser() {
        // Set up (jperezTweet cargado en beforeEach, ningún usuario tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createRetweet(999999, jperezTweet.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToCreateARetweetOfANonexistentTweet() {
        // Set up (agomez cargado en beforeEach, ningún tweet tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createRetweet(agomezId, 999999))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToCreateARetweetForADeletedUser() {
        // Set up
        twitterService.deleteUser(agomezId);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createRetweet(agomezId, jperezTweet.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToCreateARetweetOfADeletedTweet() {
        // Set up
        twitterService.deleteTweet(jperezTweet.getId());

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createRetweet(agomezId, jperezTweet.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldKeepARetweetVisibleShowingItsOriginAsUnavailableAfterTheOriginTweetIsDeleted() {
        // Set up: agomez retwitea el tweet de jperez
        twitterService.createRetweet(agomezId, jperezTweet.getId());

        // Desarrollo: se borra el tweet original (no el retweet en sí)
        twitterService.deleteTweet(jperezTweet.getId());

        // Evaluación: el retweet de agomez sigue existiendo, mostrando el origen como no disponible
        var retweets = twitterService.listRetweetsForUserId(agomezId);
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void shouldShowTheRetweetsOriginalAuthorAsADeletedAccountAfterItsOwnerDeletesTheAccount() {
        // Set up: agomez retwitea el tweet de jperez
        twitterService.createRetweet(agomezId, jperezTweet.getId());

        // Desarrollo: se borra la cuenta de jperez (autor del tweet original)
        twitterService.deleteUser(jperezId);

        // Evaluación
        var retweets = twitterService.listRetweetsForUserId(agomezId);
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("Cuenta eliminada");
    }

    @Test
    void shouldDeleteAnExistingRetweet() {
        // Set up
        twitterService.createRetweet(agomezId, jperezTweet.getId());
        Integer retweetId = twitterService.listRetweetsForUserId(agomezId).get(0).id();

        // Desarrollo
        twitterService.deleteRetweet(retweetId);

        // Evaluación
        assertThat(twitterService.listRetweetsForUserId(agomezId)).isEmpty();
    }

    @Test
    void shouldDoNothingWhenDeletingANonexistentRetweet() {
        // Set up (base sin ningún retweet con id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> twitterService.deleteRetweet(999999)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnOnlyTheAuthorsRetweets() {
        // Set up
        twitterService.createRetweet(agomezId, jperezTweet.getId());

        // Desarrollo
        var retweets = twitterService.listRetweetsForUserId(agomezId);

        // Evaluación
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).authorUsername()).isEqualTo("agomez");
    }

    // --- Follows ---

    @Test
    void shouldCreateAFollowRelationship() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        twitterService.follow(agomezId, jperezId);

        // Evaluación
        assertThat(twitterService.isFollowing(agomezId, jperezId)).isTrue();
    }

    @Test
    void shouldFailToFollowWhenTheFollowerDoesNotExist() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.follow(999999, jperezId))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToFollowWhenTheFollowedDoesNotExist() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.follow(agomezId, 999999))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToFollowWhenTheFollowerIsDeleted() {
        // Set up
        twitterService.deleteUser(agomezId);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.follow(agomezId, jperezId))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldFailToFollowWhenTheFollowedIsDeleted() {
        // Set up
        twitterService.deleteUser(jperezId);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.follow(agomezId, jperezId))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldRemoveAFollowRelationshipOnUnfollow() {
        // Set up
        twitterService.follow(agomezId, jperezId);

        // Desarrollo
        twitterService.unfollow(agomezId, jperezId);

        // Evaluación
        assertThat(twitterService.isFollowing(agomezId, jperezId)).isFalse();
    }

    @Test
    void shouldFailToUnfollowWhenTheFollowerDoesNotExist() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.unfollow(999999, jperezId))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldReturnFalseWhenThereIsNoFollowRelationship() {
        // Set up (usuarios cargados en beforeEach, sin relación de seguimiento entre ellos)

        // Desarrollo
        boolean isFollowing = twitterService.isFollowing(agomezId, jperezId);

        // Evaluación
        assertThat(isFollowing).isFalse();
    }

    @Test
    void shouldReturnTheFollowersOfAUser() {
        // Set up
        twitterService.follow(agomezId, jperezId);

        // Desarrollo
        var followers = twitterService.getFollowers(jperezId);

        // Evaluación
        assertThat(followers).hasSize(1);
        assertThat(followers.get(0).username()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnTheUsersSomeoneIsFollowing() {
        // Set up
        twitterService.follow(agomezId, jperezId);

        // Desarrollo
        var following = twitterService.getFollowing(agomezId);

        // Evaluación
        assertThat(following).hasSize(1);
        assertThat(following.get(0).username()).isEqualTo("jperez");
    }
}
