package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SimpleCodeObject {
    @SerializedName("name") final String name;
    @SerializedName("path") final String path;
}
