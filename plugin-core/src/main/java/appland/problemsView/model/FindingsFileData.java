package appland.problemsView.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FindingsFileData {
    @SerializedName("findings")
    public @Nullable List<ScannerFinding> findings;

    @SerializedName("checks")
    public @Nullable List<CheckInfo> checks;

    public @NotNull Map<String, RuleInfo> createRuleInfoMapping() {
        if (checks == null || checks.isEmpty()) {
            return Collections.emptyMap();
        }

        var result = new HashMap<String, RuleInfo>();
        for (var check : checks) {
            var rule = check.ruleInfo;
            if (rule != null) {
                result.put(check.id, rule);
            }
        }
        return result;
    }
}
