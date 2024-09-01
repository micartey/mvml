package me.micartey.mvml;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.micartey.mvml.commons.Streams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Setter
@Getter
@Accessors(chain = true)
public class MvmlConfiguration {

    private final File file;

    private String template;
    private boolean createBackup;
    private int spaces = 2;

    public MvmlConfiguration(File file) {
        this.file = file;
    }

    public MvmlParser load() throws IOException {
        MvmlParser parser = new MvmlParser(this);

        createBackup: {
            if (!this.createBackup)
                break createBackup;

            Files.copy(
                    this.file.toPath(),
                    new File(this.file.getParent(), this.file.getName() + ".backup").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        loadTemplate: {
            if (this.template == null)
                break loadTemplate;

            if (!this.file.exists()) {
                file.createNewFile();

                Files.write(file.toPath(), Streams.getValues(MvmlConfiguration.class.getResourceAsStream("/" + this.template)));
                break loadTemplate;
            }

            parser.parseFile();

            Files.write(file.toPath(), Streams.getValues(MvmlConfiguration.class.getResourceAsStream("/" + this.template)));

            parser.migrate();
            parser.save();
        }

        parser.parseFile();
        return parser;
    }
}