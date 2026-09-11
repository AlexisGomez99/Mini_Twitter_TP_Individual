package minitwitter.backend.main;

import minitwitter.backend.service.TwitterService;
import minitwitter.backend.util.EmfBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("default")
public class AppConfiguration {

    @Bean
    public TwitterService twitterService() {
        var emf = new EmfBuilder()
                .postgres()
                .withUpdateDDL()
                .build();
        return new TwitterService(emf);
    }
}
