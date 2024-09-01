package me.micartey.mvml;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.micartey.mvml.commons.FileUtilities;
import me.micartey.mvml.nodes.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

@RequiredArgsConstructor
public class MvmlParser {

    private static final Map<File, Node> FILE_BUFFER = new HashMap<>();

    private final MvmlConfiguration configuration;

    void parseFile() throws IOException {
        Node root = new RootNode();

        readLine: for (String line : FileUtilities.readFile(this.configuration.getFile())) {
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
                    for (int depth = 0; depth < index / this.configuration.getSpaces() - 1; depth++) {
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
                int index = line.indexOf(":");

                String key = line.substring(0, index).trim();
                String value = line.substring(index +  1).trim();

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

        FILE_BUFFER.put(this.configuration.getFile(), root);
    }

    /**
     * Search a node in hirachy by key
     *
     * @param key Key
     * @return Node if found - if not, it will throw a RuntimeException
     */
    private Node getNodeByKey(String key) {
        Node root = FILE_BUFFER.get(this.configuration.getFile());

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
     * Get the value of a key and convert it to a certain type
     *
     * @param key Key
     * @param type object class
     * @return instance of T
     * @param <T> object type
     */
    public <T> T get(String key, Class<T> type) {
        return (T) this.convert(type, this.get(key));
    }


    /**
     * Delete a node by key
     *
     * @param key Key to node
     */
    public void remove(String key) {
        Node target = getNodeByKey(key);

        int index = key.lastIndexOf(":");

        // Remove from root node
        if (index < 0) {
            FILE_BUFFER.get(this.configuration.getFile()).getChildren().remove(target);
            return;
        }

        String keyPart = key.substring(0, index);
        getNodeByKey(keyPart).getChildren().remove(target);
    }

    /**
     * Get all key and values as pairs (to store and iterate)
     *
     * @return list of pairs
     */
    public List<Map.Entry<String, String>> readAll() {
        Node root = FILE_BUFFER.get(this.configuration.getFile());
        return this.readNode(root, null);
    }

    /**
     * Set a value for a key.
     * If the path is not present, it will create all required Nodes
     * {@link MvmlParser#createNodePath(Node, String)}
     *
     * @param key Key
     * @param value Value
     */
    public void set(String key, Object value) {
        Node root = FILE_BUFFER.get(this.configuration.getFile());

        Node child = createNodePath(root, key);

        if (!(child instanceof LeafNode))
            throw new RuntimeException("Node is not final: " + child.getKey());

        ((LeafNode) child).setValue(String.valueOf(value));
    }

    /**
     * Recursive method to get all keys of a (sub-)tree
     *
     * @param node parent Node
     * @param prefix current key path
     * @return list of keys
     */
    private List<String> toString(Node node, String prefix) {
        LinkedList<String> list = new LinkedList<>();

        if (node instanceof TextNode)
            list.add(node.toData());
        else if (!(node instanceof RootNode))
            list.add(prefix + node.toData());

        for (Node child : node.getChildren()) {
            StringBuilder indents = new StringBuilder();

            for (int i = 0; i < this.configuration.getSpaces(); i++)
                indents.append(" ");

            list.addAll(toString(child, prefix + (node instanceof RootNode ? "" : indents)));
        }

        return list;
    }

    /**
     * Overwrite the file on disc with information stored in memory
     */
    @SneakyThrows
    public void save() {
        Node root = FILE_BUFFER.get(this.configuration.getFile());

        List<String> lines = new ArrayList<>(toString(root, ""));

        FileUtilities.writeFile(this.configuration.getFile(), lines);
    }

    /**
     * Overwrite the memory with information stored on disc
     */
    @SneakyThrows
    public void read() {
        this.parseFile();
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

    private Object convert(Class<?> type, String name) {
        if (name.equals("null"))
            return null;

        try {
            if (type.equals(List.class)) {
                String data = name.replaceAll("^\\[|]$", "");

                if (data.isEmpty())
                    return new ArrayList<>();

                return Arrays.asList(data.split(", "));
            }

            String className = type.equals(long.class) ? "java.lang.Long" : type.equals(int.class) ? "java.lang.Integer" : type.equals(double.class) ? "java.lang.Double" : type.equals(float.class) ? "java.lang.Float" : type.equals(byte.class) ? "java.lang.Byte" : type.equals(boolean.class) ? "java.lang.Boolean" : type.equals(short.class) ? "java.lang.Short" : type.getName();
            Method method = Class.forName(className).getMethod("valueOf", String.class);
            return method.invoke(null, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        return indents / this.configuration.getSpaces();
    }

}