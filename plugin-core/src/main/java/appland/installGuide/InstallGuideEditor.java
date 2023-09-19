package appland.installGuide;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.actions.GenerateOpenApiAction;
import appland.cli.AppLandCommandLineService;
import appland.index.AppMapMetadata;
import appland.index.IndexedFileListenerUtil;
import appland.installGuide.projectData.ProjectDataService;
import appland.installGuide.projectData.ProjectMetadata;
import appland.oauth.AppMapLoginAction;
import appland.problemsView.FindingsViewTab;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectSettingsService;
import appland.settings.AppMapSettingsListener;
import appland.telemetry.TelemetryService;
import appland.webviews.WebviewEditor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.AsyncDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * {@link WebviewEditor}, which displays the AppMap installation guide.
 */
public class InstallGuideEditor extends WebviewEditor<List<ProjectMetadata>> {
    private static final @NotNull Logger LOG = Logger.getInstance(InstallGuideEditor.class);

    // currently displayed page
    private @NotNull InstallGuideViewPage type;
    // to debounce the JS refresh of available AppMaps
    private final SingleAlarm projectRefreshAlarm = new SingleAlarm(this::refreshProjects, 500, this, Alarm.ThreadToUse.POOLED_THREAD);
    // to debounce the JS refresh when settings change
    private final SingleAlarm settingsRefreshAlarm = new SingleAlarm(this::refreshSettings, 500, this, Alarm.ThreadToUse.POOLED_THREAD);

    public InstallGuideEditor(@NotNull Project project,
                              @NotNull VirtualFile file,
                              @NotNull InstallGuideViewPage type) {
        super(project, file, Set.of("click-link", "open-file", "open-page", "view-problems", "clipboard",
                "perform-install", "perform-auth", "generate-openapi"));
        this.type = type;
    }

    public void navigateTo(@NotNull InstallGuideViewPage page, boolean postWebviewMessage) {
        this.type = page;
        if (page == InstallGuideViewPage.RuntimeAnalysis) {
            AppMapProjectSettingsService.getState(project).setInvestigatedFindings(true);
        }

        if (postWebviewMessage) {
            postMessage(createPageNavigationJSON(page));
        }
    }

    private void refreshProjects() {
        postMessage(createUpdateProjectsMessage());
    }

    private void refreshSettings() {
        postMessage(createUpdateSettingsMessage());
    }

    private void setupListeners() {
        IndexedFileListenerUtil.registerListeners(project, this, true, true, projectRefreshAlarm::cancelAndRequest);

        ApplicationManager.getApplication().getMessageBus()
                .connect(this)
                .subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
                    @Override
                    public void apiKeyChanged() {
                        settingsRefreshAlarm.cancelAndRequest();
                    }

                    @Override
                    public void createOpenApiChanged() {
                        settingsRefreshAlarm.cancelAndRequest();
                    }

                    @Override
                    public void openedAppMapChanged() {
                        settingsRefreshAlarm.cancelAndRequest();
                    }

                    @Override
                    public void investigatedFindingsChanged() {
                        settingsRefreshAlarm.cancelAndRequest();
                    }
                });
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("installGuide.editor.title");
    }

    @Override
    protected @NotNull Path getApplicationFile() {
        return AppMapPlugin.getInstallGuideHTMLPath();
    }

    @Override
    protected @Nullable List<ProjectMetadata> createInitData() {
        return findProjects();
    }

    @Override
    protected void setupInitMessage(@Nullable List<ProjectMetadata> initData, @NotNull JsonObject payload) {
        addBaseProperties(payload);
        payload.add("projects", gson.toJsonTree(initData));
        payload.add("disabledPages", new JsonArray());
    }

    @Override
    protected void afterInit(@Nullable List<ProjectMetadata> initData) {
        setupListeners();
    }

    @Override
    protected @Nullable Gson createCustomizedGson() {
        return new GsonBuilder()
                .registerTypeAdapter(AppMapMetadata.class, new AppMapMetadataWebAppSerializer())
                .create();
    }

    @Override
    protected void handleMessage(@NotNull String messageId, @Nullable JsonObject message) throws Exception {
        switch (messageId) {
            case "click-link":
                assert message != null;
                handleMessageClickLink(message);
                break;

            case "open-file":
                assert message != null;
                handleMessageOpenFile(message);
                break;

            case "open-page":
                assert message != null;
                handleMessageOpenPage(message);
                break;

            case "view-problems":
                handleMessageViewProblems();
                break;

            case "clipboard": {
                assert message != null;
                handleMessageClipboard(message);
                break;
            }

            case "perform-install": {
                assert message != null;
                handleMessagePerformInstall(message);
                break;
            }

            case "perform-auth": {
                handleMessagePerformAuth();
                break;
            }

            case "generate-openapi": {
                assert message != null;
                handleMessageGenerateOpenApi(message);
                break;
            }

            default:
                LOG.warn("Unhandled message type: " + type);
        }
    }

    private @NotNull JsonObject createPageNavigationJSON(@NotNull InstallGuideViewPage page) {
        var json = createMessageObject("page");
        json.addProperty("page", page.getPageId());
        return json;
    }

    private @NotNull JsonObject createUpdateProjectsMessage() {
        var json = createMessageObject("projects");
        json.add("projects", gson.toJsonTree(findProjects()));
        return json;
    }

    private @NotNull JsonObject createUpdateSettingsMessage() {
        // we're reusing the init properties to avoid duplicate code,
        // the "settings" handler of installGuideView.js is only applying supported properties
        var settings = createMessageObject("settings");
        addBaseProperties(settings);
        settings.add("projects", gson.toJsonTree(findProjects()));
        return settings;
    }

    @NotNull
    private List<ProjectMetadata> findProjects() {
        return ProjectDataService.getInstance(project).getAppMapProjects();
    }

    /**
     * Adds the basic JSON properties to the object, which are expected by the webview application.
     *
     * @param json {@link JsonObject} to modify
     */
    private void addBaseProperties(@NotNull JsonObject json) {
        var settings = AppMapApplicationSettingsService.getInstance();

        json.addProperty("page", type.getPageId());
        json.addProperty("userAuthenticated", settings.getApiKey() != null);
        json.addProperty("analysisEnabled", true);
    }

    private void handleMessageClickLink(@NotNull JsonObject message) {
        if (message.has("uri")) {
            openExternalLink(message.getAsJsonPrimitive("uri").getAsString());
        }
    }

    private void handleMessagePerformAuth() {
        AppMapLoginAction.authenticate();
        TelemetryService.getInstance().sendEvent("analysis:cta_interaction");
    }

    private void handleMessageGenerateOpenApi(@NotNull JsonObject message) {
        var projectPath = message.has("projectPath") ? message.getAsJsonPrimitive("projectPath").getAsString() : null;
        var appMapRoot = StringUtil.isNotEmpty(projectPath)
                ? LocalFileSystem.getInstance().findFileByNioFile(Paths.get(projectPath))
                : null;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (appMapRoot != null) {
                GenerateOpenApiAction.createOpenApiFile(project, appMapRoot, false);
            } else {
                GenerateOpenApiAction.createOpenApiFileInteractive(project, false);
            }
        }, ModalityState.defaultModalityState());
    }

    private void handleMessagePerformInstall(@NotNull JsonObject message) {
        var path = message.getAsJsonPrimitive("path").getAsString();
        var language = message.getAsJsonPrimitive("language").getAsString();
        executeInstallCommand(path, language);
    }

    private void handleMessageClipboard(@NotNull JsonObject message) {
        // fixme send telemetry, as in VSCode?
        var content = message.getAsJsonPrimitive("text").getAsString();
        LOG.debug("Copying text to clipboard: " + content);

        var target = new StringSelection(content);
        ClipboardSynchronizer.getInstance().setContent(target, target);
    }

    private void handleMessageViewProblems() {
        ApplicationManager.getApplication().invokeLater(() -> {
            FindingsViewTab.activateFindingsTab(project);
        });
    }

    private void handleMessageOpenFile(@NotNull JsonObject message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            var path = Paths.get(message.getAsJsonPrimitive("file").getAsString());
            var file = LocalFileSystem.getInstance().findFileByNioFile(path);
            if (file != null) {
                FileEditorManager.getInstance(project).openFile(file, true);
            }
        });
    }

    private void handleMessageOpenPage(@NotNull JsonObject message) {
        var allProjects = findProjects();

        var viewId = message.getAsJsonPrimitive("page").getAsString();
        var viewProject = gson.fromJson(message.get("project"), ProjectMetadata.class);
        var anyInstallable = allProjects.stream().anyMatch(p -> p.getScore() >= 2);
        var projectLanguage = viewProject != null ? viewProject.getLanguage() : null;
        var projectLanguageName = projectLanguage != null && projectLanguage.getName() != null
                ? projectLanguage.getName().toLowerCase()
                : "";

        TelemetryService.getInstance().sendEvent(
                "view:open",
                (event) -> event
                        .property("appmap.view.id", viewId)
                        .property("appmap.project.language", projectLanguageName)
                        .property("appmap.project.installable", String.valueOf(viewProject != null && viewProject.getScore() >= 2))
                        .property("appmap.project.any_installable", String.valueOf(anyInstallable))
        );
    }

    private void executeInstallCommand(String path, String language) {
        var commandLine = AppLandCommandLineService.getInstance().createInstallCommand(Paths.get(path), language);
        if (commandLine == null) {
            return;
        }

        // follows com.intellij.ide.actions.runAnything.activity.RunAnythingCommandProvider.runCommand,
        // but uses our own PtyCommandLine instead
        ApplicationManager.getApplication().invokeLater(() -> {
            var runAnythingRunProfile = new RunAnythingRunProfile(commandLine, commandLine.getCommandLineString()) {
                @Override
                public @NotNull RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
                    return new AppMapRunProfileState(commandLine, environment);
                }
            };

            var dataContext = (AsyncDataContext) dataId -> {
                if (CommonDataKeys.PROJECT.is(dataId)) {
                    return project;
                }
                return null;
            };

            try {
                var executor = DefaultRunExecutor.getRunExecutorInstance();
                ExecutionEnvironmentBuilder.create(project, executor, runAnythingRunProfile)
                        .dataContext(dataContext)
                        .buildAndExecute();
            } catch (ExecutionException e) {
                LOG.warn("Failed to execute command: " + commandLine.getCommandLineString(), e);
            }
        });
    }
}
