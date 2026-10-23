package minitwitter.backend.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String ORIGEN_FRONTEND = "http://localhost:5173";

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/**")
                .allowedOrigins(ORIGEN_FRONTEND)
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
