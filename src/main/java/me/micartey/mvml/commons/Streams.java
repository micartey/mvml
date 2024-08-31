package me.micartey.mvml.commons;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Streams {

    @SneakyThrows
    public static ArrayList<String> getValues(InputStream inputStream) {
        ArrayList<String> values = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        reader.lines().forEach(values::add);

        return values;
    }
}