package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FindingsFileData {
    @SerializedName("findings")
    public @Nullable List<ScannerFinding> findings;
}
