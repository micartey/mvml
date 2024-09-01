import me.micartey.mvml.MvmlConfiguration;
import me.micartey.mvml.MvmlParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class DeleteTest {


    private static final File FILE = new File("src/test/resources/temp.yml");

    @Test
    public void testDelete() throws IOException {
        MvmlParser parser = new MvmlConfiguration(FILE)
                .setTemplate("example.yml")
                .load();

        parser.remove("License");
    }
}
