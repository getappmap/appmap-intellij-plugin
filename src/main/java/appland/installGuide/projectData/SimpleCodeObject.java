package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class SimpleCodeObject {
    @NotNull @SerializedName("name") final String name;
    @Nullable @SerializedName("path") final String path;
}
