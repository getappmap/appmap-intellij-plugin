package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

/**
 * Single metadata value of property "appMapMetadata" of appmap-findings.json files.
 */
@EqualsAndHashCode
public final class FindingsMetadata {
    @SerializedName("name")
    public @Nullable String appMapName = null;

    @SerializedName("test_status")
    public @Nullable TestStatus testStatus = null;
}
