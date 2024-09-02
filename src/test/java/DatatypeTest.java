import me.micartey.mvml.MvmlConfiguration;
import me.micartey.mvml.MvmlParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DatatypeTest {

    private static final File FILE = new File("src/test/resources/temp.yml");

    private MvmlParser parser;

    @BeforeEach
    public void setUp() throws IOException {
        this.parser = new MvmlConfiguration(FILE)
                .load();
    }

    @Test
    public void testList() {
        List<String> values = Arrays.asList("Test", "123");

        this.parser.set("test", values);

        Assertions.assertArrayEquals(values.toArray(), this.parser.get("test", List.class).toArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 4})
    public void testInt(int testValue) {
        this.parser.set("test", testValue);

        int value = this.parser.get("test", int.class);

        Assertions.assertEquals(value, testValue);
    }

    @ParameterizedTest
    @ValueSource(floats = {1.2f, 3.3453f, 4.234235f, Float.MAX_VALUE})
    public void testFloat(float testValue) {
        this.parser.set("test", testValue);

        float value = this.parser.get("test", float.class);

        Assertions.assertEquals(value, testValue);
    }
}
