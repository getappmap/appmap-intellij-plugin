package appland.projectPicker;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Languages {
    private static final Object lock = new Object();
    private static List<Language> languages = null;
    private static Map<String, Language> extensionMapping = null;

    static {
        setup();
    }

    public static List<Language> getLanguages() {
        return languages;
    }

    public static @Nullable Language getLanguage(@NotNull String extension) {
        return extensionMapping.get(extension);
    }

    private static void setup() {
        var gson = new GsonBuilder().create();
        var resource = Languages.class.getResourceAsStream("/projectPicker/languages.json");
        var languageArray = gson.fromJson(new InputStreamReader(resource), Language[].class);
        languages = Arrays.asList(languageArray);

        var mapping = new HashMap<String, Language>();
        for (Language language : languages) {
            for (String extension : language.extensions) {
                mapping.put(extension, language);
            }
        }
        extensionMapping = mapping;
    }

    public static final class Language {
        @SerializedName("id")
        public String id;
        @SerializedName("name")
        public String name;
        @SerializedName("extensions")
        public String[] extensions;
    }
}
