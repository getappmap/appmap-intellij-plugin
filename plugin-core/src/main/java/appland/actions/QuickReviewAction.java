package appland.actions;

import appland.webviews.navie.NavieEditorProvider;
import appland.webviews.navie.NaviePromptSuggestion;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class QuickReviewAction extends AnAction implements DumbAware {
    public static final String ACTION_ID = "appmap.quickReview";
    private static final String LAST_PICKED_REF_KEY = "appmap.quickReview.lastPickedRef";
    private static final List<String> COMMON_MAIN_BRANCHES = Arrays.asList(
            "main", "master", "develop", "release", "staging", "testing", "qa", "prod"
    );

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        var repositoryManager = GitRepositoryManager.getInstance(project);
        var repositories = repositoryManager.getRepositories();
        if (repositories.isEmpty()) {
            return;
        }

        // For now, we'll just use the first repository
        var repository = repositories.get(0);

        new Task.Backgroundable(project, "Fetching Git Refs", true) {
            private List<GitRef> refs;
            private boolean dirty = false;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    refs = getItems(project, repository);
                    dirty = isDirty();
                } catch (VcsException ex) {
                    throw new RuntimeException("Failed to fetch Git refs", ex);
                }
            }

            private boolean isDirty() {
                var handler = new GitLineHandler(project, repository.getRoot(), GitCommand.STATUS);
                handler.setSilent(true);
                handler.addParameters("--porcelain");
                try {
                    var result = Git.getInstance().runCommand(handler);
                    result.throwOnError();
                    return !result.getOutput().isEmpty();
                } catch (VcsException e) {
                    return false; // If we can't check, assume not dirty
                }
            }

            @Override
            public void onSuccess() {
                if (refs == null || refs.isEmpty()) {
                    return;
                }

                var head = repository.getInfo().getCurrentRevision();
                var lastPickedRef = PropertiesComponent.getInstance().getValue(LAST_PICKED_REF_KEY);
                AtomicBoolean seenLastPicked = new AtomicBoolean(false);

                refs = refs.stream()
                        /* only show HEAD if the repository is dirty */
                        .filter(gitRef -> dirty || !gitRef.commit.equals(head))
                        .peek(gitRef -> {
                            if (gitRef.commit.equals(head)) gitRef.description = "review uncommitted changes";
                            if (gitRef.label.equals(lastPickedRef)) {
                                gitRef.description = "last picked ⋅ " + gitRef.description;
                                seenLastPicked.set(true);
                            }
                        })
                        .sorted(Comparator.comparing((GitRef gitRef) ->
                                // if we have seen the last picked ref, sort it to the start
                                // otherwise show common main branches first
                                seenLastPicked.get() ? !gitRef.label.equals(lastPickedRef)
                                        : !COMMON_MAIN_BRANCHES.contains(gitRef.label)))
                        .toList();

                var popup = JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(refs)
                        .setRenderer(new GitRefCellRenderer())
                        .setTitle("Select base for review")
                        .setMovable(false)
                        .setResizable(false)
                        .setRequestFocus(true)
                        .setItemChosenCallback((selectedValue) -> {
                            if (selectedValue != null) {
                                PropertiesComponent.getInstance().setValue(LAST_PICKED_REF_KEY, selectedValue.label);
                                NavieEditorProvider.openEditorWithPrompt(project, new NaviePromptSuggestion(
                                        "Quick Review",
                                        String.format("@review /base=%s", selectedValue.label)));
                            }
                        }).createPopup();
                popup.showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    private ArrayList<GitRef> getRefs(Project project, GitRepository repository) throws VcsException {
        var refs = new ArrayList<GitRef>();
        var handler = new GitCommandLineHandler(project, repository.getRoot(), "for-each-ref");
        handler.addParameters(
                "--format=%(objectname);%(if)%(HEAD)%(then)HEAD%(else)%(refname:short)%(end);%(refname:rstrip=-2);%(objectname:short) ⋅ %(creatordate:human)",
                "--merged", "HEAD", "--sort=-creatordate");
        var result = Git.getInstance().runCommand(handler);
        result.throwOnError();

        refs.addAll(result.getOutput().stream()
                .map(GitRef::ofLine)
                .toList());
        return refs;
    }

    private List<GitRef> getItems(Project project, GitRepository repository) throws VcsException {
        var refs = getRefs(project, repository);
        var head = repository.getInfo().getCurrentRevision();
        var nextRefIdx = refs.stream()
                .filter(gitRef -> !gitRef.commit.equals(head))
                .findFirst()
                .map(refs::indexOf)
                .orElse(-1);
        if (nextRefIdx > -1) {
            // add commits up to the next ref
            var nextCommit = refs.get(nextRefIdx).commit;
            var handler = new GitLineHandler(project, repository.getRoot(), GitCommand.LOG);
            handler.addParameters("--format=%H;%h;commit;%ch ⋅ %s", nextCommit + "...HEAD");
            var result = Git.getInstance().runCommand(handler);
            result.throwOnError();
            var commits = result.getOutput().stream()
                    .map(GitRef::ofLine)
                    .filter(gitRef -> !gitRef.commit.equals(head)) // Exclude HEAD commit
                    .toList();
            if (!commits.isEmpty()) {
                // Insert commits before the next ref
                refs.addAll(nextRefIdx, commits);
            }
        }
        return refs;
    }

    private static class GitRef {
        private static final Pattern PATTERN = Pattern.compile("^(.*?);(.*?);(.*?);(.*)$");
        String commit;
        String label;
        String type;
        String description;

        GitRef(String commit, String label, String type, String description) {
            this.commit = commit;
            this.label = label;
            this.type = type;
            this.description = description;
        }

        public static GitRef ofLine(String line) {
            var matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                return new GitRef(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
            }
            throw new IllegalArgumentException("Invalid ref line: " + line);
        }

        @Override
        public String toString() {
            return label + " (" + type + ") - " + commit + " - " + description;
        }

        public Icon getIcon() {
            switch (label) {
                case "main":
                case "master":
                    return AllIcons.Actions.Checked;
                case "develop":
                    return AllIcons.Actions.Edit;
                case "release":
                    return AllIcons.Actions.Download;
                case "staging":
                    return AllIcons.Actions.Upload;
                case "testing":
                    return AllIcons.Actions.Refresh;
                case "qa":
                    return AllIcons.Actions.Execute;
                case "prod":
                    return AllIcons.Actions.Restart;
                case "HEAD":
                    return AllIcons.Actions.Pause;
            }
            switch (type) {
                case "refs/heads":
                    return AllIcons.Vcs.Branch;
                case "refs/remotes":
                    return AllIcons.Nodes.PpWeb;
                case "refs/tags":
                    return AllIcons.Nodes.Bookmark;
                default:
                    return AllIcons.Vcs.CommitNode;
            }
        }
    }

    private static class GitRefCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            var component = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof GitRef) {
                var ref = (GitRef) value;
                component.setText("<html><b>" + ref.label + "</b> " +
                        "<small>" + ref.description + "</small></html>");
                component.setIcon(ref.getIcon());
            }
            return component;
        }
    }

    // HACK: platform version 241 does not define GitCommand.FOR_EACH_REF
    // and the final GitCommand constructors are private, so we need to replace
    // the command in GitLineHandler with a custom one.
    class GitCommandLineHandler extends GitLineHandler {
        public GitCommandLineHandler(@Nullable Project project, @NotNull VirtualFile directory, @NotNull String command) {
            super(project, directory, GitCommand.LOG);
            var paramsList = this.myCommandLine.getParametersList();
            var index = paramsList.getParameters().indexOf(GitCommand.LOG.name());
            if (index != -1) {
                paramsList.set(index, command);
            } else {
                paramsList.addAt(0, command);
            }
        }
    }
}