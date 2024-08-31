package me.micartey.mvml;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.micartey.mvml.commons.FileUtilities;
import me.micartey.mvml.commons.Streams;
import me.micartey.mvml.nodes.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Setter
@Getter
@Accessors(chain = true)
public class MvmlConfiguration {

    private static final Map<File, Node> FILE_BUFFER = new HashMap<>();

    private final File file;

    private String template;
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

        loadTemplate: {
            if (this.template == null)
                break loadTemplate;

            if (!this.file.exists()) {
                file.createNewFile();

                Files.write(file.toPath(), Streams.getValues(MvmlConfiguration.class.getResourceAsStream("/" + this.template)));
                break loadTemplate;
            }

            this.parseFile();
            this.migrate();
            this.save();
        }

        this.parseFile();
    }

    private void parseFile() throws IOException {
        Node root = new RootNode();

        readLine: for (String line : FileUtilities.readFile(this.file)) {
            /*
             * Root comments
             */
            if (line.startsWith("#")) {
                root.getChildren().add(new TextNode(line));
                continue;
            }

            /*
             * Empty lines
             */
            if (line.isEmpty()) {
                Node node = root.getChildren().getLast();
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
                        root.getChildren().addLast(new ParentNode(key)); // Parent Node
                        continue readLine;
                    }

                    Node parent = root.getChildren().getLast();
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

                Node node = root.getChildren().getLast();
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
                    root.getChildren().add(new LeafNode(key, value));
                    continue;
                }

                Node node = root.getChildren().getLast();
                for (int i = 0; i < indents - 1; i++)
                    node = node.getChildren().getLast();

                node.getChildren().addLast(new LeafNode(key, value));
                continue;
            }

            throw new RuntimeException("No implemention found for: " + line);
        }

        FILE_BUFFER.put(file, root);
    }

    /**
     * Search a node in hirachy by key
     *
     * @param key Key
     * @return Node if found - if not, it will throw a RuntimeException
     */
    private Node getNodeByKey(String key) {
        Node root = FILE_BUFFER.get(this.file);

        String[] keys = key.split("\\.");

        Node node = root;

        for (String keyPart : keys) {
            node = node.getChildren().stream().filter(it -> keyPart.equals(it.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Key not present: " + key));
        }

        return node;
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
            String prefix = path == null ? "" : path + ".";

            for (Node child : node.getChildren()) {
                result.addAll(readNode(child, prefix + child.getKey()));
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
        Node root = FILE_BUFFER.get(this.file);
        return this.readNode(root, null);
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
        Node root = FILE_BUFFER.get(this.file);

        Node child = createNodePath(root, key);

        if (!(child instanceof LeafNode))
            throw new RuntimeException("Node is not final: " + child.getKey());

        ((LeafNode) child).setValue(value);
    }

    private List<String> toString(Node node, String prefix) {
        LinkedList<String> list = new LinkedList<>();

        if (node instanceof TextNode)
            list.add(node.toData());
        else if (!(node instanceof RootNode))
            list.add(prefix + node.toData());

        for (Node child : node.getChildren()) {
            StringBuilder indents = new StringBuilder();

            for (int i = 0; i < this.spaces; i++)
                indents.append(" ");

            list.addAll(toString(child, prefix + (node instanceof RootNode ? "" : indents)));
        }

        return list;
    }

    @SneakyThrows
    public void save() {
        Node root = FILE_BUFFER.get(this.file);

        List<String> lines = new ArrayList<>(toString(root, ""));

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

    private long countIndents(String line) {
        long indents = 0;
        for (char c : line.toCharArray()) {
            if (c != ' ')
                break;

            indents++;
        }

        return indents / this.spaces;
    }
}