package appland.projectPicker;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class Languages {
    private static final List<Language> languages = loadLanguages();
    private static final Map<String, Language> extensionMapping = createExtensionMapping(languages);

    public static List<Language> getLanguages() {
        return languages;
    }

    public static @Nullable Language getLanguage(@NotNull String extension) {
        return extensionMapping.get(extension);
    }

    private static @NotNull Map<String, Language> createExtensionMapping(@NotNull List<Language> languages) {
        var mapping = new HashMap<String, Language>();
        for (var language : languages) {
            for (var extension : language.extensions) {
                mapping.put(StringUtil.trimStart(extension, "."), language);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Language language = (Language) o;
            return Objects.equals(id, language.id) && Objects.equals(name, language.name) && Arrays.equals(extensions, language.extensions);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(id, name);
            result = 31 * result + Arrays.hashCode(extensions);
            return result;
        }

        @Override
        public String toString() {
            return "Language{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", extensions=" + Arrays.toString(extensions) +
                    '}';
        }
    }
}
