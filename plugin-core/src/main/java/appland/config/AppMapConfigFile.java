package appland.config;

import appland.utils.YamlUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresWriteLock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static appland.utils.YamlUtils.getProperty;
import static appland.utils.YamlUtils.mappingNode;
import static appland.utils.YamlUtils.removeProperty;
import static appland.utils.YamlUtils.scalarNode;
import static appland.utils.YamlUtils.scalarValue;
import static appland.utils.YamlUtils.setProperty;
import static appland.utils.YamlUtils.toData;

/**
 * Represents a `appmap.yml` file.
 * It's operating on the parsed YAML node tree and retains all original values when written to disk:
 * values which are not modified keep their original text and quoting.
 */
public class AppMapConfigFile {
    private static final Logger LOG = Logger.getInstance(AppMapConfigFile.class);
    private static final String NAME_PROPERTY = "name";
    private static final String APPMAP_DIR_PROPERTY = "appmap_dir";
    private static final String PACKAGES_PROPERTY = "packages";
    private static final String PATH_PROPERTY = "path";
    private static final String LANGUAGE_PROPERTY = "language";
    private static final String LANGUAGE_VALUE = "java";

    private final @NotNull MappingNode yamlData;

    private AppMapConfigFile(@NotNull MappingNode yamlData) {
        this.yamlData = yamlData;
    }

    public AppMapConfigFile() {
        this(mappingNode());
        setProperty(yamlData, LANGUAGE_PROPERTY, scalarNode(LANGUAGE_VALUE));
    }

    public @Nullable String getName() {
        return scalarValue(getProperty(yamlData, NAME_PROPERTY));
    }

    public void setName(@Nullable String name) {
        if (StringUtil.isEmpty(name)) {
            removeProperty(yamlData, NAME_PROPERTY);
        } else {
            setProperty(yamlData, NAME_PROPERTY, scalarNode(name));
        }
    }

    /**
     * @return The value of the "appmap_dir" property of the AppMap configuration
     */
    public @Nullable String getAppMapDir() {
        return scalarValue(getProperty(yamlData, APPMAP_DIR_PROPERTY));
    }

    /**
     * Updates property "appmap_dir". It's removed if {@code null} is passed as new value.
     */
    public void setAppMapDir(@Nullable String appMapDir) {
        if (appMapDir == null) {
            removeProperty(yamlData, APPMAP_DIR_PROPERTY);
        } else {
            setProperty(yamlData, APPMAP_DIR_PROPERTY, scalarNode(appMapDir));
        }
    }

    public @NotNull List<Package> getPackages() {
        var yamlValue = getProperty(yamlData, PACKAGES_PROPERTY);
        if (yamlValue == null) {
            return Collections.emptyList();
        }

        if (!(yamlValue instanceof SequenceNode packageNodes)) {
            LOG.debug("Property 'packages' must be an array");
            return Collections.emptyList();
        }

        return packageNodes.getValue().stream()
                .filter(MappingNode.class::isInstance)
                .map(packageNode -> new Package(scalarValue(getProperty((MappingNode) packageNode, PATH_PROPERTY))))
                .collect(Collectors.toList());
    }

    public void setPackages(@Nullable List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            removeProperty(yamlData, PACKAGES_PROPERTY);
            return;
        }

        var packageNodes = packages.stream()
                .map(path -> {
                    var packageNode = mappingNode();
                    setProperty(packageNode, PATH_PROPERTY, scalarNode(path));
                    return (Node) packageNode;
                })
                .collect(Collectors.toList());
        setProperty(yamlData, PACKAGES_PROPERTY, new SequenceNode(Tag.SEQ, packageNodes, DumperOptions.FlowStyle.BLOCK));
    }

    public @Nullable String getLanguage() {
        return scalarValue(getProperty(yamlData, LANGUAGE_PROPERTY));
    }

    public void writeTo(@NotNull Path filePath) throws IOException {
        try (var writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            YamlUtils.newYaml().serialize(yamlData, writer);
        }
    }

    @RequiresWriteLock
    public void writeTo(@NotNull VirtualFile file) throws IOException {
        var output = new StringWriter();
        YamlUtils.newYaml().serialize(yamlData, output);
        VfsUtilCore.saveText(file, output.toString());
    }

    /**
     * Parses the content of the given file as an AppMap configuration file and returns the result.
     *
     * @param configFile VirtualFile, it's not required that it's located on the local filesystem.
     */
    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable VirtualFile configFile) {
        if (configFile == null) {
            return null;
        }

        try {
            try (var inputStream = configFile.getInputStream();
                 var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return parseConfigFile(reader);
            }
        } catch (Exception e) {
            LOG.debug("Error reading AppMap configuration: " + configFile, e);
            return null;
        }
    }

    /**
     * Parses the content of the given file as an AppMap configuration file and returns the result.
     */
    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable Path configFile) {
        if (configFile == null) {
            return null;
        }

        try (var inputStream = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            return parseConfigFile(inputStream);
        } catch (Exception e) {
            LOG.debug("Error reading AppMap configuration: " + configFile, e);
            return null;
        }
    }

    /**
     * A simple implementation to parse the needed config data from a Reader.
     */
    private static @Nullable AppMapConfigFile parseConfigFile(@NotNull Reader configFile) {
        try {
            var yamlTree = YamlUtils.newYaml().compose(configFile);
            if (!(yamlTree instanceof MappingNode mappingNode)) {
                LOG.debug("appmap.yml is not an object");
                return null;
            }

            return new AppMapConfigFile(mappingNode);
        } catch (Exception e) {
            LOG.debug("Error reading appmap configuration", e);
            return null;
        }
    }

    /**
     * Two configurations are equal if they hold the same data, regardless of the order of the properties
     * and of how the values are formatted in the file.
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppMapConfigFile other && toData(yamlData).equals(toData(other.yamlData));
    }

    @Override
    public int hashCode() {
        return toData(yamlData).hashCode();
    }

    @Override
    public String toString() {
        return "AppMapConfigFile(" + toData(yamlData) + ")";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Package {
        @Nullable String path = null;
    }
}
