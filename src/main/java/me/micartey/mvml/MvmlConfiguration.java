package me.micartey.mvml;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.micartey.mvml.commons.ConsoleColors;
import me.micartey.mvml.commons.FileUtilities;
import me.micartey.mvml.nodes.LeafNode;
import me.micartey.mvml.nodes.Node;
import me.micartey.mvml.nodes.ParentNode;
import me.micartey.mvml.nodes.TextNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Setter
@Getter
@Accessors(chain = true)
public class MvmlConfiguration {

    private static final Map<File, List<Node>> FILE_BUFFER = new HashMap<>();

    private final File file;

    private boolean createBackup;
    private int spaces = 2;

    public MvmlConfiguration(File file) {
        this.file = file;
    }

    public void load() throws IOException {
        createBackup: {
            if (!this.createBackup)
                break createBackup;

            Files.copy(
                    this.file.toPath(),
                    new File(this.file.getParent(), this.file.getName() + ".backup").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        this.parseFile();
    }

    private void parseFile() throws IOException {
        LinkedList<Node> nodes = new LinkedList<>();

        readLine: for (String line : FileUtilities.readFile(this.file)) {
            /*
             * Root comments
             */
            if (line.startsWith("#")) {
                nodes.add(new TextNode(line));
                continue;
            }

            /*
             * Empty lines
             */
            if (line.isEmpty()) {
                Node node = nodes.getLast();
                while (!node.getChildren().isEmpty())
                    node = node.getChildren().getLast();

                node.getChildren().add(new TextNode(line));
                continue;
            }

            /*
             * Parent node
             */
            if (line.endsWith(":")) {
                char[] chars = line.toCharArray();

                // TODO: Redo
                for (int index = 0; index < chars.length; index++) {
                    if (!isAlphabetic(chars[index]))
                        continue;

                    String key = line.substring(index, line.length() - 1);

                    if (index == 0) {
                        nodes.addLast(new ParentNode(key)); // Parent Node
                        continue readLine;
                    }

                    Node parent = nodes.getLast();
                    for (int depth = 0; depth < index / this.spaces - 1; depth++) {
                        parent = parent.getChildren().getLast();
                    }

                    parent.getChildren().add(new ParentNode(key));

                    continue readLine;
                }

                continue;
            }

            /*
             * Comments with indents
             */
            if (line.contains("#")) {
                long indents = this.countIndents(line);

                Node node = nodes.getLast();
                for (int i = 0; i < indents - 1; i++)
                    node = node.getChildren().getLast();

                node.getChildren().addLast(new TextNode(line));
                continue;
            }

            /*
             * Value nodes
             */
            if (line.contains(":")) {
                String[] split = line.split(":");

                String key = split[0].trim();
                String value = split[1].trim();

                long indents = this.countIndents(line);

                // Root level value node
                if (indents == 0) {
                    nodes.add(new LeafNode(key, value));
                    continue;
                }

                // Non root level value node

                Node node = nodes.getLast();
                for (int i = 0; i < indents - 1; i++)
                    node = node.getChildren().getLast();

                node.getChildren().addLast(new LeafNode(key, value));
                continue;
            }

            throw new RuntimeException("No implemention found for: " + line);
        }

        FILE_BUFFER.put(file, nodes);
    }

    /**
     * Search a node in hirachy by key
     *
     * @param key Key
     * @return Node if found - if not, it will throw a RuntimeException
     */
    private Node getNodeByKey(String key) {
        List<Node> nodes = FILE_BUFFER.get(this.file);

        String[] keys = key.split("\\.");

        Node match = nodes.stream().filter(node -> keys[0].equals(node.getKey()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Key not present: " + key));

        for (int index = 1; index < keys.length; index++) {
            String keyPart = keys[index];

            match = match.getChildren().stream().filter(node -> keyPart.equals(node.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Key not present: " + key));
        }

        return match;
    }


    /**
     * Create nodes for a path
     *
     * @param parent root
     * @param key Key
     * @return LeafNode
     */
    private Node createNodePath(Node parent, String key) {
        if (key.isEmpty()) {
            return null;
        }

        int index = key.indexOf(".");
        String currentKey;

        if (index != -1) {
            currentKey = key.substring(0, index);
        } else {
            currentKey = key;
        }

        Node current = parent.getChildren().stream().filter(child -> currentKey.equals(child.getKey()))
                .findFirst()
                .orElse(null);

        if (current == null) {
            if (index == -1) {
                current = new LeafNode(currentKey, null);
                parent.getChildren().addLast(current);
                return current;
            } else {
                current = new ParentNode(currentKey);
                parent.getChildren().addLast(current);
                return this.createNodePath(current, key.substring(index + 1));
            }
        }

        if (index == -1) {
            return current;
        }

        return this.createNodePath(current, key.substring(index + 1));
    }

    private List<Map.Entry<String, String>> readNode(Node node, String path) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        if (!(node instanceof LeafNode)) {
            for (Node child : node.getChildren()) {
                result.addAll(readNode(child, path + "." + child.getKey()));
            }

            return result;
        }

        LeafNode leafNode = (LeafNode) node;

        result.add(new AbstractMap.SimpleEntry<>(path, leafNode.getValue()));
        return result;
    }

    /**
     * Get the value of a key
     *
     * @param key Key
     * @return value (String)
     */
    public String get(String key) {
        Node match = getNodeByKey(key);

        if (!(match instanceof LeafNode))
            throw new RuntimeException("Node is not FinalNode: " + match.getKey());

        return ((LeafNode) match).getValue();
    }

    /**
     * Get all key and values as pairs (to store and iterate)
     *
     * @return list of pairs
     */
    public List<Map.Entry<String, String>> readAll() {
        List<Node> nodes = FILE_BUFFER.get(this.file);

        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Node node : nodes) {
            result.addAll(this.readNode(node, node.getKey()));
        }

        return result;
    }

    /**
     * Set a value for a key.
     * If the path is not present, it will create all required Nodes
     * {@link MvmlConfiguration#createNodePath(Node, String)}
     *
     * @param key Key
     * @param value Value
     */
    public void set(String key, String value) {
        List<Node> nodes = FILE_BUFFER.get(this.file);

        int index = key.indexOf(".");
        String keyParent = key.substring(0, index > 0 ? index : key.length());

        Node parent = nodes.stream().filter(node -> keyParent.equals(node.getKey()))
                .findFirst()
                .orElseGet(() -> {
                    if (key.contains(".")) {
                        Node node = new ParentNode(keyParent);
                        nodes.add(node);
                        return node;
                    }

                    Node node = new LeafNode(keyParent, null);
                    nodes.add(node);
                    return node;
                });

        if (index > 0) {
            parent = createNodePath(parent, key.substring(Math.min(index + 1, key.length())));
        }

        if (!(parent instanceof LeafNode))
            throw new RuntimeException("Node is not final: " + parent.getKey());

        ((LeafNode) parent).setValue(value);
    }

    private List<String> toString(Node node, String prefix) {
        LinkedList<String> list = new LinkedList<>();

        if (node instanceof TextNode)
            list.add(node.toData());
        else
            list.add(prefix + node.toData());

        for (Node child : node.getChildren()) {
            StringBuilder indents = new StringBuilder(" ");
            for (int i = 1; i < this.spaces; i++)
                indents.append(" ");

            list.addAll(toString(child, prefix + indents));
        }

        return list;
    }

    public void save() throws Exception {
        List<Node> nodes = FILE_BUFFER.get(this.file);

        List<String> lines = new ArrayList<>();

        for (Node node : nodes) {
            lines.addAll(toString(node, ""));
        }

        FileUtilities.writeFile(this.file, lines);
    }

    /**
     * Migrate the current values to a new file.
     * This expects that the file has been replaced after loading.
     * It will attempt to re-read the file and override the values with the old values.
     * New keys will not be overwritten
     *
     * @throws IOException if something happens while parsing
     */
    public void migrate() throws IOException {
        List<Map.Entry<String, String>> entries = this.readAll();

        this.parseFile();

        entries.forEach(entry -> {
            set(entry.getKey(), entry.getValue());
        });
    }

    private boolean isAlphabetic(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public long countIndents(String line) {
//        return line.chars().filter(c -> c == '\t').count();

        long indents = 0;
        for (char c : line.toCharArray()) {
            if (c != ' ')
                break;

            indents++;
        }

        return indents / this.spaces;
    }
}