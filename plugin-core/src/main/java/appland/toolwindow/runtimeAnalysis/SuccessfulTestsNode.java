package appland.toolwindow.runtimeAnalysis;

import appland.AppMapBundle;
import appland.problemsView.ScannerProblem;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.LeafState;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

final class SuccessfulTestsNode extends Node {
    private final List<ScannerProblem> problems;

    public SuccessfulTestsNode(@NotNull Project project,
                               @Nullable NodeDescriptor parentDescriptor,
                               List<ScannerProblem> problems) {
        super(project, parentDescriptor);
        this.problems = problems;
    }

    @Override
    public @NotNull LeafState getLeafState() {
        return LeafState.NEVER;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(AppMapBundle.get("runtimeAnalysis.node.successfulTests.label"));
    }

    @Override
    public List<? extends Node> getChildren() {
        var now = Instant.now();
        var byDateBucket = problems.stream().collect(Collectors.groupingBy(problem -> {
            var finding = problem.getFinding();
            var date = finding.eventsModifiedDate != null ? finding.eventsModifiedDate : finding.scopeModifiedDate;
            var ageMillis = date != null ? ChronoUnit.MILLIS.between(date.toInstant(), now) : null;
            return DateBucket.findBucket(ageMillis);
        }, TreeMap::new, Collectors.toList()));

        return byDateBucket.entrySet().stream()
                .map(entry -> new DateBucketNode(myProject, this, entry.getKey().label, entry.getValue()))
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    private enum DateBucket {
        // values are sorted by max age (which is also the display order),
        // so that the first item with "age <= maxAge" is the matching bucket of a finding
        Day(AppMapBundle.get("runtimeAnalysis.node.successfulTests.day"), TimeUnit.HOURS.toMillis(24)),
        Week(AppMapBundle.get("runtimeAnalysis.node.successfulTests.week"), TimeUnit.DAYS.toMillis(7)),
        Month(AppMapBundle.get("runtimeAnalysis.node.successfulTests.month"), TimeUnit.DAYS.toMillis(30)),
        Oldest(AppMapBundle.get("runtimeAnalysis.node.successfulTests.oldest"), Long.MAX_VALUE),
        WithoutDate(AppMapBundle.get("runtimeAnalysis.node.successfulTests.withoutDate"), -1);

        private final @NotNull String label;
        private final long maxAgeMillis;

        static @NotNull DateBucket findBucket(@Nullable Long ageMillis) {
            if (ageMillis == null || ageMillis < 0) {
                return WithoutDate;
            }

            for (var bucket : values()) {
                if (ageMillis <= bucket.maxAgeMillis) {
                    return bucket;
                }
            }

            throw new IllegalStateException("unable to find date bucket for value " + ageMillis);
        }
    }
}
