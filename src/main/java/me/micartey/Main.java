package me.micartey;

import me.micartey.mvml.MvmlConfiguration;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        File file = new File("example.yml");

        MvmlConfiguration mvmlConfiguration = new MvmlConfiguration(file)
                .setCreateBackup(false)
                .setSpaces(2);

        mvmlConfiguration.load();

        System.out.println(mvmlConfiguration.get("Anticheat.experimental"));
        System.out.println(mvmlConfiguration.get("Anticheat.targets.player"));

        System.out.println(mvmlConfiguration.readAll());

        mvmlConfiguration.migrate();

        mvmlConfiguration.save();
    }
}