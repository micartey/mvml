import me.micartey.mvml.MvmlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ExampleTest {

    private static File FILE = new File("src/test/resources/example.yml");

    @Test
    public void test() throws Exception {
        MvmlConfiguration configuration = new MvmlConfiguration(FILE);

        configuration.load();

        configuration.set("test2.a", "true");
        configuration.save();

        Assertions.assertEquals(configuration.get("Whitelist.startup.enable"), "true");
    }

}
