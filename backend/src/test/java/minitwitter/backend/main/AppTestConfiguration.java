package minitwitter.backend.main;

import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.util.EmfBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Reemplaza a AppConfiguration durante los tests de integración (@ActiveProfiles("test")):
// mismo EmfBuilder, pero contra H2 en memoria en vez de la Postgres real. Acá sí exponemos
// el EntityManagerFactory como bean propio (a diferencia de AppConfiguration) porque los
// tests de controllers necesitan @Autowired EntityManagerFactory para armar sus datos de
// prueba directo — mismo motivo por el que el profesor separa esto solo en su
// AppTestConfiguration y no en su AppConfiguration de producción.
@Configuration
@Profile("test")
public class AppTestConfiguration {

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        return new EmfBuilder()
                .h2Memory()
                .withDropAndCreateDDL()
                .build();
    }

    @Bean
    public TwitterService twitterService(EntityManagerFactory emf) {
        return new TwitterService(emf);
    }
}
