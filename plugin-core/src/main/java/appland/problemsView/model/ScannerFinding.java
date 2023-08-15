package appland.problemsView.model;

import appland.files.FileLocation;
import appland.files.FileLookup;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode
public class ScannerFinding {
    @SerializedName("hash")
    public @Nullable String appMapHash = null;

    @SerializedName("hash_v2")
    public @Nullable String appMapHashV2 = null;

    @SerializedName("ruleId")
    public @NotNull String ruleId = "";

    @SerializedName("ruleTitle")
    public @NotNull String ruleTitle = "";

    @SerializedName("stack")
    public @NotNull List<String> stack = Collections.emptyList();

    @SerializedName("message")
    public @NotNull String message = "";

    @SerializedName("groupMessage")
    public @NotNull String groupMessage = "";

    @SerializedName("impactDomain")
    public @Nullable ImpactDomain impactDomain = null;

    @SerializedName("event")
    public @Nullable ScannerFindingEvent event = null;

    @SerializedName("relatedEvents")
    public @Nullable List<ScannerFindingEvent> relatedEvents = null;

    @SerializedName("eventsModifiedDate")
    public @Nullable Date eventsModifiedDate = null;

    @SerializedName("scopeModifiedDate")
    public @Nullable Date scopeModifiedDate = null;

    // attached after JSON parsing, not defined in appmap-findings.json
    public transient @Nullable RuleInfo ruleInfo = null;

    // attached after JSON parsing, links to the source appmap-findings.file containing this finding
    @Getter
    @Setter
    private transient @Nullable VirtualFile findingsFile;

    // attached after JSON parsing, links to the source appmap-findings.file metadata
    @Getter
    @Setter
    private transient @Nullable FindingsMetadata findingsMetaData;

    public @Nullable String getAppMapHashWithFallback() {
        return StringUtil.nullize(StringUtil.defaultIfEmpty(appMapHashV2, appMapHash));
    }

    public @NotNull String getFindingTitle() {
        var rule = ruleTitle;
        var context = StringUtil.defaultIfEmpty(groupMessage, message);
        return !rule.equals(context) && !context.startsWith(rule)
                ? rule + ": " + context
                : context;
    }

    public @Nullable String getDescription() {
        if (StringUtil.isNotEmpty(message)) {
            return message;
        }
        if (StringUtil.isNotEmpty(groupMessage)) {
            return groupMessage;
        }
        return null;
    }

    public @Nullable FileLocation getProblemLocation() {
        if (stack.isEmpty()) {
            return null;
        }

        var candidate = stack.stream().filter(path -> !FileLookup.isAbsolutePath(path)).findFirst().orElse(null);
        if (candidate == null) {
            return null;
        }

        return FileLocation.parse(candidate);
    }

    public @Nullable VirtualFile findAnnotatedFile(@NotNull Project project, @NotNull VirtualFile findingsFile) {
        var parentDir = findingsFile.getParent();
        var location = getProblemLocation();
        if (location != null) {
            return FileLookup.findRelativeFile(project, parentDir, location.filePath);
        }
        return null;
    }

    public @Nullable Integer getEventId() {
        return event != null ? event.id : null;
    }
}
