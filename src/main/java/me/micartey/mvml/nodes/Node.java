package me.micartey.mvml.nodes;

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

}