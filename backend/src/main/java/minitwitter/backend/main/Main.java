package minitwitter.backend.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"minitwitter.backend.*"}) // escanea ese paquete las clases y las instancia
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);

	}

}
