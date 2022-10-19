package appland.installGuide.projectData;

import appland.index.AppMapMetadata;
import appland.problemsView.model.FindingsDomainCount;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
@Builder
public class ProjectMetadata {
    @SerializedName("name")
    @NotNull String name;

    @SerializedName("path")
    @NotNull String path;

    @SerializedName("score")
    int score;

    @SerializedName("hasNode")
    boolean hasNode;

    // needed for the findings page
    @SerializedName("analysisPerformed")
    boolean analysisPerformed;

    @SerializedName("appMapOpened")
    boolean appMapOpened;

    @SerializedName("numFindings")
    @Nullable Integer numFindings;

    @SerializedName("findingsDomainCounts")
    @Nullable FindingsDomainCount findingsDomainCounts;

    @SerializedName("numHttpRequests")
    @Nullable Integer numHttpRequests;

    @SerializedName("language")
    @Nullable ProjectMetadataFeature language;

    @SerializedName("testFramework")
    @Nullable ProjectMetadataFeature testFramework;

    @SerializedName("webFramework")
    @Nullable ProjectMetadataFeature webFramework;

    @SerializedName("numAppMaps")
    @Nullable Integer numAppMaps;

    @SerializedName("appMaps")
    @Nullable List<AppMapMetadata> appMaps;

    @SerializedName("sampleCodeObjects")
    @Nullable SampleCodeObjects sampleCodeObjects;
}
