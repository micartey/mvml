package me.micartey.mvml.nodes;

import lombok.Getter;

@Getter
public class ParentNode extends Node {

    public ParentNode(String key) {
        super(key);
    }

    @Override
    public String toData() {
        return this.getKey() + ":";
    }
}
