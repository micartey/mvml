package me.micartey.mvml.nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;

@Getter
@RequiredArgsConstructor
public abstract class Node {

    @Setter private final LinkedList<Node> children = new LinkedList<>();
    private final String key;

    public boolean containsKey(String key) {
        return key.equals(this.key) || this.children.stream().anyMatch(node -> node.containsKey(key));
    }

    public abstract String toData();

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}