package appland.copilotChat;

import appland.cli.AppLandModelInfoProvider;
import appland.copilotChat.copilot.CopilotModelDefinition;
import appland.copilotChat.copilot.GitHubCopilotService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CopilotModelInfoProvider implements AppLandModelInfoProvider {
    @Override
    public @Nullable List<ModelInfo> getModelInfo() throws IOException {
        if (!GitHubCopilotService.getInstance().isAvailable()) {
            return null;
        }

        var chatSession = GitHubCopilotService.getInstance().createChatSession();
        var filteredModels = withoutDuplicateModels(chatSession != null ? chatSession.loadChatModels() : null);
        if (filteredModels.isEmpty()) {
            return null;
        }

        var nameCounts = filteredModels.stream()
                .collect(Collectors.groupingBy(CopilotModelDefinition::name, Collectors.counting()));
        var baseUrl = NavieCopilotChatRequestHandler.getBaseUrl();
        var apiKey = GitHubCopilotService.RandomIdeSessionId;
        return filteredModels.stream().map(model -> new ModelInfo(
                nameCounts.get(model.name()) > 1
                        ? String.format("%s (%s)", model.name(), model.version())
                        : model.name(),
                model.id(),
                "Copilot",
                Instant.now().toString(),
                baseUrl,
                apiKey,
                model.capabilities().limits().maxPromptTokens()
        )).toList();
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
}
