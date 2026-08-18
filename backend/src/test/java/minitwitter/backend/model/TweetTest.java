package minitwitter.backend.model;

import minitwitter.backend.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TweetTest {

    @Test
    void deberiaCrearUnTweetOriginalValido() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "Este es mi primer tweet";

        // Desarrollo
        Tweet tweet = new Tweet(author, content);

        // Evaluación
        assertThat(tweet.getAuthor()).isEqualTo(author);
        assertThat(tweet.getContent()).isEqualTo(content);
        assertThat(tweet.getCreatedAt()).isNotNull();
        assertThat(tweet.isRetweet()).isFalse();
    }

    @Test
    void deberiaFallarAlCrearUnTweetSinTexto() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Tweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void deberiaFallarAlCrearUnTweetConMasDeDoscientosOchentaCaracteres() {
        // Set up
        User author = new User("agomez", "password123");
        String content = "a".repeat(281);

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Tweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void deberiaFallarAlCrearUnTweetSinAutor() {
        // Set up
        User author = null;
        String content = "Contenido válido";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Tweet(author, content))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener un autor.");
    }

    @Test
    void deberiaCrearUnRetweetValidoAPartirDeUnTweetDeOtroUsuario() {
        // Set up
        User originalAuthor = new User("agomez", "password123");
        User retweeter = new User("jperez", "password456");
        Tweet originalTweet = new Tweet(originalAuthor, "Tweet original");

        // Desarrollo
        Tweet retweet = new Tweet(retweeter, originalTweet);

        // Evaluación
        assertThat(retweet.getAuthor()).isEqualTo(retweeter);
        assertThat(retweet.getContent()).isNull();
        assertThat(retweet.getOrigin()).isEqualTo(originalTweet);
        assertThat(retweet.isRetweet()).isTrue();
    }

    @Test
    void deberiaFallarAlIntentarHacerRetweetDeUnTweetDelMismoAutor() {
        // Set up
        User author = new User("agomez", "password123");
        Tweet originalTweet = new Tweet(author, "Tweet original");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Tweet(author, originalTweet))
                .isInstanceOf(DomainException.class)
                .hasMessage("No podés hacer re-tweet de tu propio tweet.");
    }
}
