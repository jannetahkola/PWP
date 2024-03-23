package fi.jannetahkola.palikka.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PalikkaGameApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PalikkaGameApiApplication.class, args);
    }

}
