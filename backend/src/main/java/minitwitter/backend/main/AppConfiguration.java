package minitwitter.backend.main;

import minitwitter.backend.service.TwitterService;
import minitwitter.backend.util.EmfBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("default")
public class AppConfiguration {

    // aca se crea la instancia para poder pasarsela a otra clase y se lo pasa por constructor
    @Bean
    public TwitterService servicioTwitter() {
        var emf = new EmfBuilder()
                .postgres()
                .conActualizacionDeEsquema()
                .construir();
        return new TwitterService(emf);
    }
}
