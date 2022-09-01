package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SampleCodeObjects {
    @SerializedName("httpRequests")
    List<SimpleCodeObject> httpRequests;

    @SerializedName("queries")
    List<SimpleCodeObject> queries;
}
