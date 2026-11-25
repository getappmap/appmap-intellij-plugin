package appland.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public final class YamlUtils {
    private YamlUtils() {
    }

    /**
     * Creates a SnakeYAML instance which writes block-style YAML with indented sequences, like the files the AppMap
     * tools generate. It constructs plain data only, never arbitrary Java objects.
     * <p>
     * A new instance is needed for every use because SnakeYAML instances are not thread-safe.
     */
    public static @NotNull Yaml newYaml() {
        var dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setIndentWithIndicator(true);

        var loaderOptions = new LoaderOptions();
        return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
    }

    /**
     * @return The value of the given key in a YAML mapping, or {@code null} if there is none.
     */
    public static @Nullable Node getProperty(@NotNull MappingNode mapping, @NotNull String key) {
        return mapping.getValue().stream()
                .filter(tuple -> isKey(tuple, key))
                .map(NodeTuple::getValueNode)
                .reduce((first, second) -> second) // the last of duplicate keys wins, as when constructing data
                .orElse(null);
    }

    /**
     * Sets the value of the given key in a YAML mapping, in place of an existing value or else as the last entry.
     * Duplicates of the key are removed, so that {@link #getProperty} returns the new value.
     */
    public static void setProperty(@NotNull MappingNode mapping, @NotNull String key, @NotNull Node value) {
        var tuples = mapping.getValue();
        var newTuple = new NodeTuple(scalarNode(key), value);
        for (var i = 0; i < tuples.size(); i++) {
            if (isKey(tuples.get(i), key)) {
                tuples.set(i, newTuple);
                tuples.subList(i + 1, tuples.size()).removeIf(tuple -> isKey(tuple, key));
                return;
            }
        }
        tuples.add(newTuple);
    }

    public static void removeProperty(@NotNull MappingNode mapping, @NotNull String key) {
        mapping.getValue().removeIf(tuple -> isKey(tuple, key));
    }

    private static boolean isKey(@NotNull NodeTuple tuple, @NotNull String key) {
        return tuple.getKeyNode() instanceof ScalarNode keyNode && key.equals(keyNode.getValue());
    }

    /**
     * @return The text of a scalar value, or {@code null} if the node is missing, null or not a scalar.
     */
    public static @Nullable String scalarValue(@Nullable Node node) {
        return node instanceof ScalarNode scalar && !Tag.NULL.equals(scalar.getTag()) ? scalar.getValue() : null;
    }

    /**
     * Creates an empty block-style YAML mapping.
     */
    public static @NotNull MappingNode mappingNode() {
        return new MappingNode(Tag.MAP, new ArrayList<>(), DumperOptions.FlowStyle.BLOCK);
    }

    /**
     * Creates a string value. It's written as a plain scalar, unless it needs quotes to stay a string.
     */
    public static @NotNull ScalarNode scalarNode(@NotNull String value) {
        return new ScalarNode(Tag.STR, value, null, null, DumperOptions.ScalarStyle.PLAIN);
    }

    /**
     * Converts the node tree into plain data for comparison: maps, lists and tagged scalar values.
     */
    public static @Nullable Object toData(@NotNull Node node) {
        if (node instanceof ScalarNode scalar) {
            return Tag.NULL.equals(scalar.getTag()) ? null : new Scalar(scalar.getTag(), scalar.getValue());
        }
        if (node instanceof SequenceNode sequence) {
            return sequence.getValue().stream().map(YamlUtils::toData).collect(Collectors.toList());
        }
        if (node instanceof MappingNode mapping) {
            var data = new LinkedHashMap<Object, Object>();
            mapping.getValue().forEach(tuple -> data.put(toData(tuple.getKeyNode()), toData(tuple.getValueNode())));
            return data;
        }
        return node.toString();
    }

    private record Scalar(@NotNull Tag tag, @NotNull String value) {
    }
}
