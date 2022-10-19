package appland.installGuide.projectData;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SampleCodeObjects {
    @SerializedName("httpRequests")
    private List<SimpleCodeObject> httpRequests;

    @SerializedName("queries")
    private List<SimpleCodeObject> queries;
}
