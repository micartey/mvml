import me.micartey.mvml.MvmlConfiguration;
import me.micartey.mvml.MvmlParser;
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
        MvmlParser parser = new MvmlConfiguration(FILE)
                .setTemplate("example.yml")
                .load();

        parser.set("License", "123123");

        Assertions.assertEquals(parser.get("License"), "123123");
    }

    @AfterEach
    public void tearDown() {
        FILE.delete();
    }
}