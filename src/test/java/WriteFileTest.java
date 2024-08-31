import lombok.val;
import me.micartey.mvml.MvmlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class WriteFileTest {

    private static final File TEST_FILE = new File("src/test/resources/test.yml");

    private MvmlConfiguration configuration;

    @BeforeEach
    public void prepare() throws IOException {
        TEST_FILE.createNewFile();

        this.configuration = new MvmlConfiguration(TEST_FILE)
                .setSpaces(2);

        this.configuration.load();
    }

    @Test
    public void testWriteEmptyFile() throws Exception {
        configuration.set("test.test", "123");

        configuration.save();
        configuration.load();

        Assertions.assertEquals(configuration.get("test.test"), "123");
    }

    @Test
    public void testOverwrite() throws Exception {
        configuration.set("test.test", "123");
        configuration.save();

        val entries = configuration.readAll();
        System.out.println(entries);

        configuration.set("test.test", "456");
        configuration.save();

        entries.forEach(entry -> {
            configuration.set(entry.getKey(), entry.getValue());
        });

        configuration.save();

        Assertions.assertEquals(configuration.get("test.test"), "123");
    }

    @AfterEach
    public void cleanUp() {
        TEST_FILE.delete();
    }
}