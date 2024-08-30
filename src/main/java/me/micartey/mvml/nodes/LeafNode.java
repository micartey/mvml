package me.micartey.mvml.nodes;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LeafNode extends Node {

    @Setter private String value;

    public LeafNode(String key, String value) {
        super(key);

        this.value = value;
    }

    @Override
    public String toData() {
        return this.getKey() + ": " + getValue();
    }
}
