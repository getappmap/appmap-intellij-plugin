package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;

public enum ImpactDomain {
    @SerializedName("Security")
    Security,
    @SerializedName("Performance")
    Performance,
    @SerializedName("Maintainability")
    Maintainability,
    @SerializedName("Stability")
    Stability;
}
