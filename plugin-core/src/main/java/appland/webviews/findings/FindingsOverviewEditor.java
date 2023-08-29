package appland.webviews.findings;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.problemsView.FindingsManager;
import appland.problemsView.FindingsViewTab;
import appland.problemsView.model.ImpactDomain;
import appland.problemsView.model.ScannerFinding;
import appland.telemetry.TelemetryService;
import appland.utils.GsonUtils;
import appland.webviews.WebviewEditor;
import appland.webviews.findingDetails.FindingDetailsEditorProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FindingsOverviewEditor extends WebviewEditor<List<ScannerFinding>> {
    public FindingsOverviewEditor(@NotNull Project project, @NotNull VirtualFile file) {
        super(project, file, Set.of("open-problems-tab", "open-finding-info"));
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getFindingsAppHTMLPath();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("webview.findingsOverview.title");
    }

    @Override
    protected void setupInitMessage(@Nullable List<ScannerFinding> initData, @NotNull JsonObject payload) {
        assert initData != null;

        payload.addProperty("page", "finding-overview");
        payload.add("data", GsonUtils.singlePropertyObject("findings", createFindingsArray(initData)));
    }

    @Override
    protected void afterInit(@Nullable List<ScannerFinding> initData) {
        assert initData != null;

        var deduplicatedFindings = deduplicateFindingsByHash(initData);
        var ruleIds = findUniqueRuleIds(initData);
        var impactDomainsLowercase = findUniqueImpactDomainsLowercase(initData);
        var impactDomainCounts = createImpactDomainCounts(initData);
        var uniqueImpactDomainCounts = createImpactDomainCounts(deduplicatedFindings);

        TelemetryService.getInstance().sendEvent("analysis:view_overview", event -> {
            // properties
            event.property("appmap.analysis.rules", StringUtil.join(ruleIds, ","));
            event.property("appmap.analysis.impact_domains", StringUtil.join(impactDomainsLowercase, ","));

            // metrics
            event.metric("num_findings.num_total_findings", (double) initData.size());
            event.metric("num_findings.num_unique_findings", (double) deduplicatedFindings.size());

            impactDomainCounts.forEach((domain, count) -> {
                if (count > 0) {
                    event.metric("num_findings.num_" + domain.getJsonId().toLowerCase(), count.doubleValue());
                }
            });

            uniqueImpactDomainCounts.forEach((domain, count) -> {
                if (count > 0) {
                    event.metric("num_findings.num_unique_" + domain.getJsonId().toLowerCase(), count.doubleValue());
                }
            });

            return event;
        });
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) {
        switch (messageId) {
            case "open-problems-tab":
                ApplicationManager.getApplication().invokeLater(() -> FindingsViewTab.activateFindingsTab(project));
                break;

            case "open-finding-info":
                // locate finding by hash, then open it in a new AppMap webview
                if (message != null && message.has("hash")) {
                    openFindingsByHash(message.getAsJsonPrimitive("hash").getAsString());
                }
                break;
        }
    }

    protected @NotNull List<ScannerFinding> createInitData() {
        return FindingsManager.getInstance(project).getAllFindings();
    }

    private @NotNull JsonArray createFindingsArray(@NotNull List<ScannerFinding> initData) {
        var findingsJson = new JsonArray();
        for (var finding : initData) {
            findingsJson.add(createFindingsJsonItem(finding));
        }
        return findingsJson;
    }

    private void openFindingsByHash(@NotNull String hash) {
        var findings = FindingsManager.getInstance(project).findProblemByHash(hash);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (findings.isEmpty()) {
                Messages.showErrorDialog("Unable to locate AppMap with ID", "Error Locating AppMap");
            } else {
                FindingDetailsEditorProvider.openEditor(project, findings);
            }
        });
    }

    private static @NotNull JsonObject createFindingsJsonItem(@NotNull ScannerFinding finding) {
        var jsonItem = new JsonObject();
        jsonItem.addProperty("ruleTitle", finding.ruleTitle);
        jsonItem.addProperty("hash_v2", finding.getAppMapHashWithFallback());
        var domain = finding.impactDomain;
        if (domain != null) {
            jsonItem.addProperty("impactDomain", domain.getJsonId());
        }

        var wrapper = new JsonObject();
        wrapper.add("finding", jsonItem);
        return wrapper;
    }

    private static @NotNull List<ScannerFinding> deduplicateFindingsByHash(@NotNull List<ScannerFinding> findings) {
        if (findings.isEmpty()) {
            return List.of();
        }

        var knownHashes = new HashSet<String>();
        var deduplicated = new ArrayList<ScannerFinding>();
        for (var finding : findings) {
            var hash = finding.getAppMapHashWithFallback();
            if (!knownHashes.contains(hash)) {
                knownHashes.add(hash);
                deduplicated.add(finding);
            }
        }
        return deduplicated;
    }

    private static @NotNull Collection<String> findUniqueRuleIds(@NotNull List<ScannerFinding> findings) {
        return findings.isEmpty()
                ? Collections.emptySet()
                : findings.stream().map(e -> e.ruleId).distinct().collect(Collectors.toList());
    }

    private static Collection<String> findUniqueImpactDomainsLowercase(@NotNull List<ScannerFinding> findings) {
        if (findings.isEmpty()) {
            return Collections.emptySet();
        }

        var domains = new HashSet<String>();
        for (var finding : findings) {
            if (finding.impactDomain != null) {
                domains.add(finding.impactDomain.getJsonId().toLowerCase());
            }
        }
        return domains;
    }

    private static @NotNull Object2IntMap<ImpactDomain> createImpactDomainCounts(@NotNull List<ScannerFinding> findings) {
        if (findings.isEmpty()) {
            return Object2IntMaps.emptyMap();
        }

        var mapping = new Object2IntOpenHashMap<ImpactDomain>();
        for (var finding : findings) {
            if (finding.impactDomain != null) {
                mapping.addTo(finding.impactDomain, 1);
            }
        }
        return mapping;
    }
}
