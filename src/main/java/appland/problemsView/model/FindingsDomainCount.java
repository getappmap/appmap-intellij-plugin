package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class FindingsDomainCount {
    @SerializedName("maintainability")
    int maintainability;
    @SerializedName("performance")
    int performance;
    @SerializedName("stability")
    int stability;
    @SerializedName("security")
    int security;

    public void add(@NotNull ImpactDomain impactDomain) {
        switch (impactDomain) {
            case Security:
                security++;
                break;
            case Performance:
                performance++;
                break;
            case Maintainability:
                maintainability++;
                break;
            case Stability:
                stability++;
                break;
        }
    }
}
