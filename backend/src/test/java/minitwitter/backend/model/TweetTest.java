package minitwitter.backend.model;

import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.RetweetInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TweetTest {

    @Test
    void shouldCreateAValidOriginalTweet() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "Este es mi primer tweet";

        // Desarrollo
        OriginalTweet tweet = new OriginalTweet(author, content);

        // Evaluación
        assertThat(tweet.getAuthor()).isEqualTo(author);
        assertThat(tweet.getContent()).isEqualTo(content);
        assertThat(tweet.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFailWhenCreatingATweetWithoutText() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void shouldFailWhenCreatingATweetWithMoreThanTwoHundredEightyCharacters() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "a".repeat(281);

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void shouldFailWhenCreatingATweetWithoutAnAuthor() {
        // Set up
        User author = null;
        String content = "Contenido válido";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener un autor.");
    }

    @Test
    void shouldCreateAValidRetweetFromAnotherUsersTweet() {
        // Set up
        User originalAuthor = new User("agomez", "password123");
        User retweeter = new User("jperez", "password456");
        OriginalTweet originalTweet = new OriginalTweet(originalAuthor, "Tweet original");

        // Desarrollo
        Retweet retweet = new Retweet(retweeter, originalTweet);

        // Evaluación
        assertThat(retweet.getAuthor()).isEqualTo(retweeter);
        assertThat(retweet.getOrigin()).isEqualTo(originalTweet);
        assertThat(retweet.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFailWhenTryingToRetweetOnesOwnTweet() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet originalTweet = new OriginalTweet(author, "Tweet original");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Retweet(author, originalTweet))
                .isInstanceOf(DomainException.class)
                .hasMessage("No podés hacer re-tweet de tu propio tweet.");
    }

    @Test
    void shouldConvertAnOriginalTweetToItsInfo() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(author, "contenido de prueba");

        // Desarrollo
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(tweet.getId());
        assertThat(info.authorUsername()).isEqualTo("agomez");
        assertThat(info.content()).isEqualTo("contenido de prueba");
        assertThat(info.createdAt()).isEqualTo(tweet.getCreatedAt());
    }

    @Test
    void shouldConvertARetweetToItsInfo() {
        // Set up
        User originalAuthor = new User("agomez", "password123");
        User retweeter = new User("jperez", "password456");
        OriginalTweet originalTweet = new OriginalTweet(originalAuthor, "Tweet original");

        // Desarrollo
        Retweet retweet = new Retweet(retweeter, originalTweet);
        RetweetInfo info = retweet.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(retweet.getId());
        assertThat(info.authorUsername()).isEqualTo("jperez");
        assertThat(info.originalTweetId()).isEqualTo(originalTweet.getId());
        assertThat(info.originalAuthorUsername()).isEqualTo("agomez");
        assertThat(info.originalContent()).isEqualTo("Tweet original");
        assertThat(info.createdAt()).isEqualTo(retweet.getCreatedAt());
    }

    @Test
    void shouldNotBeDeletedWhenCreated() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(author, "contenido de prueba");

        // Desarrollo / Evaluación

        assertThat(tweet.isDeleted()).isFalse();
    }

    @Test
    void shouldClearItsContentWhenMarkedAsDeleted() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(author, "contenido de prueba");

        // Desarrollo
        tweet.markAsDeleted();

        // Evaluación
        assertThat(tweet.isDeleted()).isTrue();
        assertThat(tweet.getContent()).isNull();
    }

    @Test
    void shouldShowAPlaceholderInInfoWhenTheOriginalTweetIsDeleted() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(author, "contenido de prueba");
        tweet.markAsDeleted();

        // Desarrollo
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación
        assertThat(info.content()).isEqualTo("Publicación no disponible");
        assertThat(info.authorUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldShowAPlaceholderAuthorInInfoWhenTheAuthorAccountIsDeleted() {
        // Set up
        User author = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(author, "contenido de prueba");
        // User.tweets es el lado inverso de la relación: en JPA, Hibernate lo completa
        // solo al cargar al usuario desde la base. Acá se simula esa carga a mano para
        // que markAsDeleted() encuentre este tweet al recorrer la colección.
        author.getTweets().add(tweet);
        author.markAsDeleted();

        // Desarrollo: al borrarse la cuenta, User.markAsDeleted ya marcó el tweet también
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación
        assertThat(info.authorUsername()).isEqualTo("Cuenta eliminada");
        assertThat(info.content()).isEqualTo("Publicación no disponible");
    }

    @Test
    void shouldKeepShowingARetweetWhenItsOriginIsDeletedButMarkItAsUnavailable() {
        // Set up: jperez retwitea un tweet de agomez, que luego se borra
        User originalAuthor = new User("agomez", "password123");
        User retweeter = new User("jperez", "password456");
        OriginalTweet originalTweet = new OriginalTweet(originalAuthor, "Tweet original");
        Retweet retweet = new Retweet(retweeter, originalTweet);

        // Desarrollo
        originalTweet.markAsDeleted();
        RetweetInfo info = retweet.toInfo();

        // Evaluación: el retweet sigue existiendo y apuntando al mismo origen,
        // solo que ahora se muestra como no disponible.
        assertThat(info.originalTweetId()).isEqualTo(originalTweet.getId());
        assertThat(info.originalContent()).isEqualTo("Publicación no disponible");
        assertThat(info.originalAuthorUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldShowThePlaceholderAuthorOnARetweetWhenTheOriginalAuthorAccountIsDeleted() {
        // Set up: jperez retwitea un tweet de agomez, y luego se borra la cuenta de agomez
        User originalAuthor = new User("agomez", "password123");
        User retweeter = new User("jperez", "password456");
        OriginalTweet originalTweet = new OriginalTweet(originalAuthor, "Tweet original");
        Retweet retweet = new Retweet(retweeter, originalTweet);
        // Ver comentario equivalente en shouldShowAPlaceholderAuthorInInfoWhenTheAuthorAccountIsDeleted.
        originalAuthor.getTweets().add(originalTweet);

        // Desarrollo
        originalAuthor.markAsDeleted();
        RetweetInfo info = retweet.toInfo();

        // Evaluación
        assertThat(info.originalAuthorUsername()).isEqualTo("Cuenta eliminada");
        assertThat(info.originalContent()).isEqualTo("Publicación no disponible");
    }
}
