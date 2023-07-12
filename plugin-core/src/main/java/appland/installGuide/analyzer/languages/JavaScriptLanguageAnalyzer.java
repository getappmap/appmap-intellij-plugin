package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import appland.utils.GsonUtils;
import appland.utils.YamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaScriptLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "javascript".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var features = detectFeatures(directory);
        if (features == null) {
            features = new Features(createLanguageFallback(), null, null);
        }

        return new ProjectAnalysis(directory, features);
    }

    private @Nullable Features detectFeatures(@NotNull VirtualFile directory) {
        // dependency name -> dependency version
        var dependencies = new ArrayList<Pair<String, String>>();
        loadPackageJson(directory, dependencies);
        loadPackageLock(directory, dependencies);
        loadYarn1Lock(directory, dependencies);
        loadYarn2Lock(directory, dependencies);

        if (dependencies.isEmpty()) {
            return null;
        }

        FeatureEx web = null;
        if (findDependencyVersion(dependencies, "express") != null) {
            web = new FeatureEx();
            web.title = "express.js";
            web.score = Score.Okay;
            web.text = "This project uses Express. AppMap will automatically recognize web requests, SQL queries, and key framework functions during recording.";
        }

        var test = findTestPackage(dependencies);

        var lang = new FeatureEx("package.json", "@appland/appmap-agent-js", "package");
        lang.title = "JavaScript";
        lang.score = Score.Okay;
        lang.text = "JavaScript is currently in Open Beta. Please read the docs before proceeding.";

        return new Features(lang, web, test);
    }

    private static @Nullable String findDependencyVersion(@NotNull List<Pair<String, String>> dependencies, @NotNull String name) {
        return dependencies.stream().filter(pair -> name.equals(pair.first)).findFirst().map(pair -> pair.second).orElse(null);
    }

    private @Nullable Feature findTestPackage(@NotNull List<Pair<String, String>> dependencies) {
        for (var nameWithMajorVersion : List.of(Pair.create("mocha", 8), Pair.create("jest", 25))) {
            var name = nameWithMajorVersion.first;
            var minimumMajorVersion = nameWithMajorVersion.second;
            var packageVersion = findDependencyVersion(dependencies, name);

            if (packageVersion != null) {
                var test = new Feature();
                test.title = name;

                if (isAtLeastMajorVersion(packageVersion, minimumMajorVersion)) {
                    test.score = Score.Okay;
                    test.text = String.format("This project uses %s. You can record AppMaps of your tests.", name);
                } else {
                    test.score = Score.Bad;
                    test.text = String.format(
                            "This project uses an unsupported version of %s. You need version %d to record AppMaps of your tests.",
                            name,
                            minimumMajorVersion);
                }

                return test;
            }
        }

        return null;
    }

    private @NotNull FeatureEx createLanguageFallback() {
        var feature = new FeatureEx();
        feature.title = "JavaScript";
        feature.score = Score.Okay;
        feature.text = "This project uses JavaScript. You can add AppMap to this project by creating a package.json file.";
        return feature;
    }

    private boolean isAtLeastMajorVersion(@NotNull String value, int minimumMajorVersion) {
        if ("latest".equals(value)) {
            return true;
        }

        var version = SemVer.parseFromText(StringUtil.trimStart(StringUtil.trimStart(value, "^"), "~"));
        return version != null && version.isGreaterOrEqualThan(minimumMajorVersion, 0, 0);
    }

    private void loadPackageJson(@NotNull VirtualFile directory, @NotNull List<Pair<String, String>> target) {
        var json = loadJsonFile(directory, "package.json");
        if (json != null) {
            for (var key : List.of("dependencies", "devDependencies")) {
                if (json.has(key)) {
                    for (var keyValue : json.getAsJsonObject(key).entrySet()) {
                        if (keyValue.getValue().isJsonPrimitive()) {
                            target.add(Pair.create(keyValue.getKey(), keyValue.getValue().getAsString()));
                        }
                    }
                }
            }
        }
    }

    /**
     * For example:
     * <pre><code>
     * dependencies: {
     *     "esutils": {
     *       "version": "2.0.2",
     *       "resolved": "https://registry.npmjs.org/esutils/-/esutils-2.0.2.tgz",
     *       "integrity": "sha1-Cr9PHKpbyx96nYrMbepPqqBLrJs="
     *     }
     * },
     * </code></pre>
     */
    private void loadPackageLock(@NotNull VirtualFile directory, List<Pair<String, String>> target) {
        var json = loadJsonFile(directory, "package-lock.json");
        if (json == null) {
            return;
        }

        for (var property : List.of("dependencies", "packages")) {
            if (json.has(property)) {
                for (var dependency : json.getAsJsonObject(property).entrySet()) {
                    if (dependency.getValue().isJsonObject()) {
                        var value = dependency.getValue().getAsJsonObject();
                        if (value.has("version")) {
                            // remove prefix of "node_modules/mocha" to support v3
                            var key = dependency.getKey().replace("node_modules/", "").trim();
                            target.add(Pair.create(key, value.getAsJsonPrimitive("version").getAsString()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads file "yarn.lock" of Yarn v1, which isn't valid YAML but a custom format.
     * We're implementing a very simple lookup to avoid writing a parser.
     */
    private void loadYarn1Lock(@NotNull VirtualFile directory, List<Pair<String, String>> target) {
        var lockFile = directory.findChild("yarn.lock");
        if (lockFile == null) {
            return;
        }

        var document = FileDocumentManager.getInstance().getDocument(lockFile);
        if (document == null) {
            return;
        }

        // Dependencies are
        //    "mocha@~10.2.0":
        // or
        //    mocha@~10.2.0:

        var dependencyPattern = Pattern.compile("^\"?(.+?)@(.+?)\"?:$");
        for (var line : document.getText().lines().collect(Collectors.toList())) {
            var dependencyMatch = dependencyPattern.matcher(line);
            if (dependencyMatch.matches()) {
                var name = dependencyMatch.group(1);
                var version = dependencyMatch.group(2);
                target.add(Pair.create(name, version));
            }
        }
    }

    /**
     * Reads YAML file "yarn.lock" of Yarn v2.
     */
    private void loadYarn2Lock(@NotNull VirtualFile directory, List<Pair<String, String>> target) {
        var yaml = loadYamlFile(directory, "yarn.lock");
        if (yaml instanceof ObjectNode) {
            for (var it = yaml.fields(); it.hasNext(); ) {
                var field = it.next();
                var value = field.getValue();
                if (value.isObject()) {
                    target.add(Pair.create(field.getKey(), value.get("version").asText()));
                }
            }
        }
    }

    private @Nullable JsonObject loadJsonFile(VirtualFile directory, String fileName) {
        var file = directory.findChild(fileName);
        if (file == null) {
            return null;
        }

        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return null;
        }
        return GsonUtils.GSON.fromJson(document.getText(), JsonObject.class);
    }

    private @Nullable JsonNode loadYamlFile(VirtualFile directory, String fileName) {
        var file = directory.findChild(fileName);
        if (file == null) {
            return null;
        }

        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return null;
        }
        try {
            return YamlUtils.YAML.readTree(document.getText());
        } catch (Exception e) {
            return null;
        }
    }
}
