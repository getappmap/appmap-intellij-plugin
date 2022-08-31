package appland.installGuide.analyzer.languages;

import appland.installGuide.analyzer.*;
import com.google.gson.JsonObject;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaScriptLanguageAnalyzer implements LanguageAnalyzer {
    @Override
    public boolean isAccepting(Languages.@NotNull Language language) {
        return "javascript".equals(language.id);
    }

    @Override
    public @NotNull ProjectAnalysis analyze(@NotNull VirtualFile directory) {
        var packageJson = directory.findChild("package.json");

        Features features = null;
        if (packageJson != null) {
            features = findPackageJsonFeatures(packageJson);
        }

        if (features == null) {
            features = new Features(createLanguageFallback(), null, null);
        }

        return new ProjectAnalysis(directory, features);
    }

    private @Nullable Features findPackageJsonFeatures(@NotNull VirtualFile file) {
        var document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return null;
        }

        var json = GsonUtils.GSON.fromJson(document.getText(), JsonObject.class);

        FeatureEx web = null;
        if (GsonUtils.hasProperty(json, "dependencies", "express")) {
            web = new FeatureEx();
            web.title = "express.js";
            web.score = Score.Okay;
            web.text = "This project uses Express. AppMap will automatically recognize web requests, SQL queries, and key framework functions during recording.";
        }

        FeatureEx test = null;
        var mocha = GsonUtils.getPath(json, "devDependencies", "mocha");
        if (mocha != null && mocha.isString()) {
            test = new FeatureEx();
            test.title = "mocha";

            if (isAtLeastMocha8(mocha.getAsString())) {
                test.score = Score.Okay;
                test.text = "This project uses Mocha. Test execution can be automatically recorded.";
            } else {
                test.score = Score.Bad;
                test.text = "This project uses an unsupported version of Mocha. You need at least version 8 to automatically record test execution.";
            }
        }

        var lang = new FeatureEx("package.json", "@appland/appmap-agent-js", "package");
        lang.title = "JavaScript";
        lang.score = Score.Okay;
        lang.text = "JavaScript is currently in Open Beta. Please read the docs before proceeding.";
        return new Features(lang, web, test);
    }

    private boolean isAtLeastMocha8(@NotNull String value) {
        var version = SemVer.parseFromText(StringUtil.trimStart(StringUtil.trimStart(value, "^"), "~"));
        return version != null && version.isGreaterOrEqualThan(8, 0, 0);
    }

    private @NotNull FeatureEx createLanguageFallback() {
        var feature = new FeatureEx();
        feature.title = "JavaScript";
        feature.score = Score.Okay;
        feature.text = "This looks like a JavaScript project without a dependency file. JavaScript is currently in Open Beta. Please read the docs before proceeding.";
        return feature;
    }
}
