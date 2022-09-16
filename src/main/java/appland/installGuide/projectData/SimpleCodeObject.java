package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class SimpleCodeObject {
    @NotNull @SerializedName("name") final String name;
    @Nullable @SerializedName("path") final String path;

    public @NotNull SimpleCodeObject asTruncatedObject() {
        // VSCode only returns the substring up to the first semicolon, if available
        var index = name.indexOf(';');
        if (index <= 0) {
            return this;
        }

        return new SimpleCodeObject(name.substring(0, index), path);
    }
}
