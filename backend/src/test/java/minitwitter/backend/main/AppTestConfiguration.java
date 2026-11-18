package minitwitter.backend.main;

import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.util.EmfBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class AppTestConfiguration {

    @Bean
    public EntityManagerFactory fabricaEntityManager() {
        return new EmfBuilder()
                .h2EnMemoria()
                .conRecreacionDeEsquema()
                .construir();
    }

    @Bean
    public TwitterService servicioTwitter(EntityManagerFactory emf) {
        return new TwitterService(emf);
    }
}
