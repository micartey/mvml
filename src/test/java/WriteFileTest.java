import lombok.val;
import me.micartey.mvml.MvmlConfiguration;
import me.micartey.mvml.MvmlParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class WriteFileTest {

    private static final File TEST_FILE = new File("src/test/resources/temp.yml");

    private MvmlParser parser;

    @BeforeEach
    public void setup() throws IOException {
        TEST_FILE.createNewFile();

        this.parser = new MvmlConfiguration(TEST_FILE)
                .setSpaces(2)
                .load();
    }

    @Test
    public void testWriteEmptyFile() throws Exception {
        parser.set("test.test", "123");

        parser.save();
        parser.read();

        Assertions.assertEquals(parser.get("test.test"), "123");
    }

    @Test
    public void testOverwrite() throws Exception {
        parser.set("test.test", "123");
        parser.save();

        val entries = parser.readAll();
        System.out.println(entries);

        parser.set("test.test", "456");
        parser.save();

        entries.forEach(entry -> {
            parser.set(entry.getKey(), entry.getValue());
        });

        parser.save();

        Assertions.assertEquals(parser.get("test.test"), "123");
    }

    @AfterEach
    public void cleanUp() {
        TEST_FILE.delete();
    }
}