package appland.config;

import appland.utils.YamlUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a `appmap.yml` file.
 * It's operating on the parsed YML and retains all original values when written to disk.
 */
@Data
@AllArgsConstructor
public class AppMapConfigFile {
    private static final Logger LOG = Logger.getInstance(AppMapConfigFile.class);
    private static final String NAME_PROPERTY = "name";
    private static final String APPMAP_DIR_PROPERTY = "appmap_dir";
    private static final String PACKAGES_PROPERTY = "packages";
    private static final String LANGUAGE_PROPERTY = "language";
    private static final String LANGUAGE_VALUE = "java";

    private final @NotNull ObjectNode yamlData;

    public AppMapConfigFile() {
        this(new ObjectNode(JsonNodeFactory.instance));
        yamlData.set(LANGUAGE_PROPERTY, new TextNode(LANGUAGE_VALUE));
    }

    public @Nullable String getName() {
        if (yamlData.hasNonNull(NAME_PROPERTY)) {
            return yamlData.get(NAME_PROPERTY).asText();
        }
        return null;
    }

    public void setName(@Nullable String name) {
        if (StringUtil.isEmpty(name)) {
            yamlData.remove(NAME_PROPERTY);
        } else {
            yamlData.set(NAME_PROPERTY, new TextNode(name));
        }
    }

    /**
     * @return The value of the "appmap_dir" property of the AppMap configuration
     */
    public @Nullable String getAppMapDir() {
        return yamlData.hasNonNull(APPMAP_DIR_PROPERTY)
                ? yamlData.get(APPMAP_DIR_PROPERTY).asText()
                : null;
    }

    /**
     * Updates property "appmap_dir". It's removed if {@code null} is passed as new value.
     */
    public void setAppMapDir(@Nullable String appMapDir) {
        if (appMapDir == null) {
            yamlData.remove(APPMAP_DIR_PROPERTY);
        } else {
            yamlData.set(APPMAP_DIR_PROPERTY, new TextNode(appMapDir));
        }
    }

    public @NotNull List<Package> getPackages() {
        if (!yamlData.has(PACKAGES_PROPERTY)) {
            return Collections.emptyList();
        }

        var yamlValue = yamlData.get(PACKAGES_PROPERTY);
        if (!yamlValue.isArray()) {
            LOG.debug("Property 'packages' must be an array");
            return Collections.emptyList();
        }

        try {
            return YamlUtils.YAML.readerForListOf(Package.class).readValue(yamlValue);
        } catch (IOException e) {
            LOG.debug("Error parsing appmap config packages", e);
            return Collections.emptyList();
        }
    }

    public void setPackages(@Nullable List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            yamlData.remove(PACKAGES_PROPERTY);
            return;
        }

        var packageNodes = packages.stream()
                .map(Package::new)
                .collect(Collectors.toList());
        yamlData.set(PACKAGES_PROPERTY, YamlUtils.YAML.valueToTree(packageNodes));
    }

    public @Nullable String getLanguage() {
        if (yamlData.hasNonNull(LANGUAGE_PROPERTY)) {
            return yamlData.get(LANGUAGE_PROPERTY).asText();
        }
        return null;
    }

    public void writeTo(@NotNull Path filePath) throws IOException {
        YamlUtils.YAML.writeValue(filePath.toFile(), yamlData);
    }

    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable VirtualFile configFile) {
        if (configFile == null) {
            return null;
        }

        try {
            var nioPath = configFile.toNioPath();
            return parseConfigFile(nioPath);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * A simple implementation to parse the needed config data from an appmap.yml file.
     * This is not using A YAML parse, but attempts line-matching. There's no YAML parser available in the SDK.
     */
    public static @Nullable AppMapConfigFile parseConfigFile(@Nullable Path configFile) {
        if (configFile == null) {
            return null;
        }

        try {
            var yamlTree = YamlUtils.YAML.readTree(configFile.toFile());
            if (!yamlTree.isObject()) {
                LOG.debug("appmap.yml is not an object: " + configFile);
                return null;
            }

            return new AppMapConfigFile((ObjectNode) yamlTree);
        } catch (Exception e) {
            LOG.debug("Error reading appmap configuration: " + configFile, e);
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Package {
        @JsonProperty("path")
        @Nullable String path = null;
    }
}
