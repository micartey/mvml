package me.micartey.mvml.nodes;

import lombok.Setter;

public class TextNode extends Node {

    @Setter
    private String value;

    public TextNode(String value) {
        super(null);

        this.value = value;
    }

    @Override
    public String toData() {
        return this.value;
    }

    @Override
    public String toString() {
//        if (!this.getChildren().isEmpty())
//            throw new IllegalStateException("Textnode has children: " + this.getChildren());

        return super.toString();
    }
}