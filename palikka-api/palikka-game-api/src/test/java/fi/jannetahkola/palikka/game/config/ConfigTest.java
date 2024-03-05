package fi.jannetahkola.palikka.game.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class ConfigTest {

    @Test
    void test() {
        Path path = Paths.get("home/minecraft/minecraft/", "/server.jar");
        System.out.println("path=" + path);
    }
}
