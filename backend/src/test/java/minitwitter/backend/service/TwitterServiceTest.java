package minitwitter.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;


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
        twitterService.deleteTweet(agomezId, tweetId);

        // Evaluación
        assertThat(twitterService.listTweetsForUserId(agomezId)).isEmpty();
    }

    @Test
    void shouldFailToDeleteATweetOfAnotherUser() {
        // Set up (el tweet de jperez se cargó en beforeEach)

        // Desarrollo / Evaluación: agomez no puede borrar el tweet de jperez
        assertThatThrownBy(() -> twitterService.deleteTweet(agomezId, jperezTweet.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No podés eliminar un tweet de otra persona");
        assertThat(twitterService.listTweetsForUserId(jperezId)).hasSize(1);
    }

    @Test
    void shouldDoNothingWhenDeletingANonexistentTweet() {
        // Set up (base sin ningún tweet con id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> twitterService.deleteTweet(agomezId, 999999)).doesNotThrowAnyException();
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
        twitterService.deleteTweet(jperezId, jperezTweet.getId());

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> twitterService.createRetweet(agomezId, jperezTweet.getId()))
                .isInstanceOf(DomainException.class);
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
    void shouldDoNothingWhenFollowingSomeoneAlreadyFollowed() {
        // Set up
        twitterService.follow(agomezId, jperezId);

        // Desarrollo / Evaluación: seguir de nuevo no lanza excepción ni duplica la relación
        assertThatCode(() -> twitterService.follow(agomezId, jperezId)).doesNotThrowAnyException();
        assertThat(twitterService.getFollowers(jperezId)).hasSize(1);
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

}
