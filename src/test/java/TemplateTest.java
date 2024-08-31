import me.micartey.mvml.MvmlConfiguration;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;

public class TemplateTest {

    private static final File FILE = new File("src/test/resources/temp.yml");

    @BeforeEach
    public void setUp() throws IOException {
        FILE.createNewFile();
    }

    @Test
    public void test() throws IOException {
        MvmlConfiguration configuration = new MvmlConfiguration(FILE)
                .setTemplate("example.yml");

        configuration.load();

        configuration.set("License", "123123");

        configuration.save();

        Assertions.assertEquals(configuration.get("License"), "123123");
    }

    @AfterEach
    public void tearDown() {
        FILE.delete();
    }
}