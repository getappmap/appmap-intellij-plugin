package appland.copilotChat;

import appland.AppMapBundle;
import appland.copilotChat.copilot.CopilotModelDefinition;
import appland.copilotChat.copilot.GitHubCopilotService;
import appland.settings.AppMapApplicationSettingsService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ChooseCopilotModelAction extends AnAction implements DumbAware {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var isEnabled = e.getProject() != null
                && !CopilotAppMapEnvProvider.isDisabled()
                && GitHubCopilotService.getInstance().isCopilotAuthenticated();
        e.getPresentation().setEnabled(isEnabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        try {
            var task = new Task.WithResult<List<CopilotModelDefinition>, IOException>(project, AppMapBundle.get("action.copilot.chooseModel.loadingModels"), true) {
                @Override
                protected List<CopilotModelDefinition> compute(@NotNull ProgressIndicator progressIndicator) throws IOException {
                    var chatSession = GitHubCopilotService.getInstance().createChatSession();
                    return chatSession != null ? chatSession.loadChatModels() : null;
                }
            };

            var models = ProgressManager.getInstance().run(task);
            var filteredModels = withoutDuplicateModels(models);
            if (filteredModels == null || filteredModels.isEmpty()) {
                Messages.showErrorDialog(
                        project,
                        AppMapBundle.get("action.copilot.chooseModel.noModels.text"),
                        AppMapBundle.get("action.copilot.chooseModel.noModels.title")
                );
                return;
            }

            // null value at the beginning to allow resetting the selected Copilot model
            filteredModels.add(0, null);

            var currentModel = findCurrentModel(filteredModels);
            var popup = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(filteredModels)
                    .setTitle(AppMapBundle.get("action.copilot.chooseModel.popup.title"))
                    .setRenderer(new CopilotModelRenderer(currentModel))
                    .setSelectedValue(currentModel, true)
                    .setItemsChosenCallback(selectedModels -> {
                        assert (selectedModels.size() == 1);
                        var selectedModel = selectedModels.iterator().next();
                        var selectedId = selectedModel == null ? null : selectedModel.id();
                        AppMapApplicationSettingsService.getInstance().setCopilotModelId(selectedId);
                    });
            popup.createPopup().showCenteredInCurrentWindow(project);
        } catch (IOException ex) {
            Messages.showErrorDialog(
                    project,
                    AppMapBundle.get("action.copilot.chooseModel.errorLoadingModels.text"),
                    AppMapBundle.get("action.copilot.chooseModel.errorLoadingModels.title")
            );
        }
    }

    /**
     * Remove duplicates, there are models with the same name and version, but with different IDs.
     * We're keeping the model with the shortest ID, e.g. gpt-4o instead gpt-4o-preview.
     * <p>
     * This method returns a mutable list if at least one model is available.
     */
    private static @NotNull List<CopilotModelDefinition> withoutDuplicateModels(@Nullable List<CopilotModelDefinition> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }

        var comparator = Comparator.comparing((CopilotModelDefinition model) -> model.name() + model.version())
                .thenComparing(CopilotModelDefinition::id);
        final String[] lastClassifier = {null};
        return models.stream()
                .sorted(comparator)
                .filter(model -> {
                    var classifier = model.name() + model.version();
                    try {
                        return lastClassifier[0] == null || !lastClassifier[0].equals(classifier);
                    } finally {
                        lastClassifier[0] = classifier;
                    }
                })
                .sorted(Comparator.comparing(CopilotModelDefinition::name))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static @Nullable CopilotModelDefinition findCurrentModel(Collection<CopilotModelDefinition> models) {
        // highlight the currently selected model in the list
        // awkward code, because "Stream.findFirst()" doesn't like null values inside the Optional.
        var currentModelId = AppMapApplicationSettingsService.getInstance().getCopilotModelId();
        var filteredModels = models.stream()
                .filter(model -> Objects.equals(currentModelId, model == null ? null : model.id()))
                .toList();
        return filteredModels.size() == 1 ? filteredModels.get(0) : null;
    }

    private static class CopilotModelRenderer extends ColoredListCellRenderer<CopilotModelDefinition> {
        private final @Nullable CopilotModelDefinition currentModel;

        public CopilotModelRenderer(@Nullable CopilotModelDefinition currentModelId) {
            this.currentModel = currentModelId;
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends CopilotModelDefinition> list,
                                             CopilotModelDefinition value,
                                             int index,
                                             boolean selected,
                                             boolean hasFocus) {
            var defaultAttributes = Objects.equals(value, currentModel)
                    ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                    : SimpleTextAttributes.REGULAR_ATTRIBUTES;

            if (value == null) {
                append(AppMapBundle.get("action.copilot.chooseModel.popup.defaultModel"), defaultAttributes);
                return;
            }

            append(value.name(), defaultAttributes);
            append(" (" + value.version() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
}
