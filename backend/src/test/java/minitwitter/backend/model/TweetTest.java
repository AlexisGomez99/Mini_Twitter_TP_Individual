package minitwitter.backend.model;

import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.RetweetInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TweetTest {

    @Test
    void deberiaCrearUnTweetOriginalValido() {
        // Set up
        User autor = new User("agomez", "password123");
        String contenido = "Este es mi primer tweet";

        // Desarrollo
        OriginalTweet tweet = new OriginalTweet(autor, contenido);

        // Evaluación
        assertThat(tweet.getAuthor()).isEqualTo(autor);
        assertThat(tweet.getContent()).isEqualTo(contenido);
        assertThat(tweet.getCreatedAt()).isNotNull();
    }

    @Test
    void deberiaFallarAlCrearUnTweetSinTexto() {
        // Set up
        User autor = new User("agomez", "password123");
        String contenido = "";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(autor, contenido))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void deberiaFallarAlCrearUnTweetConMasDeDoscientosOchentaCaracteres() {
        // Set up
        User autor = new User("agomez", "password123");
        String contenido = "a".repeat(281);

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(autor, contenido))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener entre 1 y 280 caracteres.");
    }

    @Test
    void deberiaFallarAlCrearUnTweetSinAutor() {
        // Set up
        User autor = null;
        String contenido = "Contenido válido";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new OriginalTweet(autor, contenido))
                .isInstanceOf(DomainException.class)
                .hasMessage("El tweet debe tener un autor.");
    }

    @Test
    void deberiaCrearUnRetweetValidoDelTweetDeOtroUsuario() {
        // Set up
        User autorOriginal = new User("agomez", "password123");
        User quienRetuitea = new User("jperez", "password456");
        OriginalTweet tweetOriginal = new OriginalTweet(autorOriginal, "Tweet original");

        // Desarrollo
        Retweet retweet = new Retweet(quienRetuitea, tweetOriginal);

        // Evaluación
        assertThat(retweet.getAuthor()).isEqualTo(quienRetuitea);
        assertThat(retweet.getOrigin()).isEqualTo(tweetOriginal);
        assertThat(retweet.getCreatedAt()).isNotNull();
    }

    @Test
    void deberiaFallarAlIntentarRetuitearElPropioTweet() {
        // Set up
        User autor = new User("agomez", "password123");
        OriginalTweet tweetOriginal = new OriginalTweet(autor, "Tweet original");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Retweet(autor, tweetOriginal))
                .isInstanceOf(DomainException.class)
                .hasMessage("No podés hacer re-tweet de tu propio tweet.");
    }

    @Test
    void deberiaConvertirUnTweetOriginalEnSuInfo() {
        // Set up
        User autor = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(autor, "contenido de prueba");

        // Desarrollo
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(tweet.getId());
        assertThat(info.authorUsername()).isEqualTo("agomez");
        assertThat(info.content()).isEqualTo("contenido de prueba");
        assertThat(info.createdAt()).isEqualTo(tweet.getCreatedAt());
    }

    @Test
    void deberiaConvertirUnRetweetEnSuInfo() {
        // Set up
        User autorOriginal = new User("agomez", "password123");
        User quienRetuitea = new User("jperez", "password456");
        OriginalTweet tweetOriginal = new OriginalTweet(autorOriginal, "Tweet original");

        // Desarrollo
        Retweet retweet = new Retweet(quienRetuitea, tweetOriginal);
        RetweetInfo info = retweet.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(retweet.getId());
        assertThat(info.authorUsername()).isEqualTo("jperez");
        assertThat(info.originalTweetId()).isEqualTo(tweetOriginal.getId());
        assertThat(info.originalAuthorUsername()).isEqualTo("agomez");
        assertThat(info.originalContent()).isEqualTo("Tweet original");
        assertThat(info.originalDeleted()).isFalse();
        assertThat(info.createdAt()).isEqualTo(retweet.getCreatedAt());
    }

    @Test
    void deberiaMostrarUnTextoDeReemplazoEnLaInfoCuandoElTweetOriginalEstaBorrado() {
        // Set up
        User autor = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(autor, "contenido de prueba");
        tweet.markAsDeleted();

        // Desarrollo
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación: la entidad conserva el texto, pero lo que se expone es el placeholder
        assertThat(tweet.getContent()).isEqualTo("contenido de prueba");
        assertThat(info.content()).isEqualTo("Publicación no disponible");
        assertThat(info.authorUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaMostrarUnAutorDeReemplazoEnLaInfoCuandoLaCuentaDelAutorEstaEliminada() {
        // Set up
        User autor = new User("agomez", "password123");
        OriginalTweet tweet = new OriginalTweet(autor, "contenido de prueba");
        // User.tweets es el lado inverso de la relación: en JPA, Hibernate lo completa
        // solo al cargar al usuario desde la base. Acá se simula esa carga a mano para
        // que markAsDeleted() encuentre este tweet al recorrer la colección.
        autor.getTweets().add(tweet);
        autor.markAsDeleted();

        // Desarrollo: al borrarse la cuenta, User.markAsDeleted ya marcó el tweet también
        OriginalTweetInfo info = tweet.toInfo();

        // Evaluación
        assertThat(info.authorUsername()).isEqualTo("Cuenta eliminada");
        assertThat(info.content()).isEqualTo("Publicación no disponible");
    }

    @Test
    void deberiaSeguirMostrandoUnRetweetCuandoSuOrigenEstaBorradoPeroMarcarloNoDisponible() {
        // Set up: jperez retwitea un tweet de agomez, que luego se borra
        User autorOriginal = new User("agomez", "password123");
        User quienRetuitea = new User("jperez", "password456");
        OriginalTweet tweetOriginal = new OriginalTweet(autorOriginal, "Tweet original");
        Retweet retweet = new Retweet(quienRetuitea, tweetOriginal);

        // Desarrollo
        tweetOriginal.markAsDeleted();
        RetweetInfo info = retweet.toInfo();

        // Evaluación: el retweet sigue existiendo y apuntando al mismo origen,
        // solo que ahora se muestra como no disponible.
        assertThat(info.originalTweetId()).isEqualTo(tweetOriginal.getId());
        assertThat(info.originalContent()).isEqualTo("Publicación no disponible");
        assertThat(info.originalAuthorUsername()).isEqualTo("agomez");
        assertThat(info.originalDeleted()).isTrue();
    }

    @Test
    void deberiaMostrarElAutorDeReemplazoEnUnRetweetCuandoLaCuentaDelAutorOriginalEstaEliminada() {
        // Set up: jperez retwitea un tweet de agomez, y luego se borra la cuenta de agomez
        User autorOriginal = new User("agomez", "password123");
        User quienRetuitea = new User("jperez", "password456");
        OriginalTweet tweetOriginal = new OriginalTweet(autorOriginal, "Tweet original");
        Retweet retweet = new Retweet(quienRetuitea, tweetOriginal);
        // Ver comentario equivalente en shouldShowAPlaceholderAuthorInInfoWhenTheAuthorAccountIsDeleted.
        autorOriginal.getTweets().add(tweetOriginal);

        // Desarrollo
        autorOriginal.markAsDeleted();
        RetweetInfo info = retweet.toInfo();

        // Evaluación
        assertThat(info.originalAuthorUsername()).isEqualTo("Cuenta eliminada");
        assertThat(info.originalContent()).isEqualTo("Publicación no disponible");
        assertThat(info.originalDeleted()).isTrue();
    }
}
