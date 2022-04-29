package appland.projectPicker;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class Languages {
    private static final List<Language> languages = loadLanguages();
    private static final Map<String, Language> extensionMapping = loadLanguageMapping(languages);

    public static List<Language> getLanguages() {
        return languages;
    }

    public static @Nullable Language getLanguage(@NotNull String extension) {
        return extensionMapping.get(extension);
    }

    private static @NotNull Map<String, Language> loadLanguageMapping(@NotNull List<Language> languages) {
        var mapping = new HashMap<String, Language>();
        for (var language : languages) {
            for (String extension : language.extensions) {
                mapping.put(extension, language);
            }
        }
        return mapping;
    }

    private static @NotNull List<Language> loadLanguages() {
        try (var resource = Languages.class.getResourceAsStream("/projectPicker/languages.json")) {
            assert resource != null;

            var gson = new GsonBuilder().create();
            return Arrays.asList(gson.fromJson(new InputStreamReader(resource), Language[].class));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Definition of a single language in the JSON data copied over from the vscode plugin, see language.json.
     */
    public static final class Language {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("extensions")
        public String[] extensions;
    }
}
