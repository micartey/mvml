package me.micartey.mvml.commons;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUtilities {

    public static void writeFile(File file, List<String> list) throws Exception {
        writeFile(file.getAbsolutePath(), list);
    }

    public static void writeFile(String filename, List<String> list) throws IOException {
        File file = new File(filename);
        file.createNewFile();

        FileOutputStream outputStream = new FileOutputStream(file);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

        for (String line : list) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.close();
    }

    public static List<String> readFile(File file) throws IOException {
        return Files.readAllLines(file.toPath());
    }
}