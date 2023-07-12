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
import com.google.gson.*;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.ide.actions.runAnything.execution.RunAnythingRunProfile;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.AsyncDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.TerminalExecutionConsole;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.SingleAlarm;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstallGuideEditor extends UserDataHolderBase implements FileEditor {
    private static final Logger LOG = Logger.getInstance("#appmap.installGuide");
    private static final String READY_MESSAGE_ID = "intellij-plugin-ready";

    private final Project project;
    private final @NotNull VirtualFile file;
    private @NotNull InstallGuideViewPage type;

    private final JCEFHtmlPanel contentPanel = new JCEFHtmlPanel(true, null, null);
    private final JBCefJSQuery jcefBridge = JBCefJSQuery.create((JBCefBrowserBase) contentPanel);

    private final AtomicBoolean navigating = new AtomicBoolean(false);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(AppMapMetadata.class, new AppMapMetadataWebAppSerializer())
            .create();

    // to debounce the JS refresh of available AppMaps
    private final SingleAlarm projectRefreshAlarm = new SingleAlarm(this::refreshProjects, 500, this);

    // to debounce the JS refresh when settings change
    private final SingleAlarm settingsRefreshAlarm = new SingleAlarm(this::refreshSettings, 500, this);

    public InstallGuideEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull InstallGuideViewPage type) {
        this.project = project;
        this.file = file;
        this.type = type;

        // contentPanel and jcefBridge register with the client as Disposable parent
        Disposer.register(this, contentPanel.getJBCefClient());

        setupListeners();
        setupJCEF();
        loadApplication();
    }

    public void navigateTo(@NotNull InstallGuideViewPage page) {
        this.type = page;
        if (page == InstallGuideViewPage.RuntimeAnalysis) {
            AppMapProjectSettingsService.getState(project).setInvestigatedFindings(true);
        }
        postMessage(createPageNavigationJSON(page));
    }

    public void refreshProjects() {
        postMessage(createUpdateProjectsMessage());
    }

    public void refreshSettings() {
        postMessage(createUpdateSettingsMessage());
    }

    private void setupListeners() {
        IndexedFileListenerUtil.registerListeners(project, this, true, true, projectRefreshAlarm::cancelAndRequest);

        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(AppMapSettingsListener.TOPIC, new AppMapSettingsListener() {
            @Override
            public void apiKeyChanged() {
                settingsRefreshAlarm.cancelAndRequest();
            }

            @Override
            public void enableFindingsChanged() {
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
        });
    }

    private void setupJCEF() {
        // open links to https://appmap.io in the external browser
        contentPanel.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                return user_gesture && openExternalLink(request.getURL());
            }
        }, contentPanel.getCefBrowser());

        contentPanel.setErrorPage((errorCode, errorText, failedUrl) -> {
            if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED && navigating.getAndSet(false)) {
                return null;
            }
            return JBCefBrowserBase.ErrorPage.DEFAULT.create(errorCode, errorText, failedUrl);
        });

        contentPanel.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
                if (READY_MESSAGE_ID.equals(message)) {
                    onJavaScriptApplicationReady();
                    return true;
                }

                var output = String.format("AppMap JS, %s:%d, %s", source, line, message);
                switch (level) {
                    case LOGSEVERITY_FATAL:
                        LOG.error(output);
                        return true;
                    // logging ERROR as warning because the AppMap app always logs errors about svg image dimensions at start
                    case LOGSEVERITY_ERROR:
                    case LOGSEVERITY_WARNING:
                        LOG.warn(output);
                        return true;
                    case LOGSEVERITY_INFO:
                        LOG.info(output);
                        return true;
                    default:
                        LOG.debug(output);
                        return true;
                }
            }
        }, contentPanel.getCefBrowser());
    }

    private void loadApplication() {
        try {
            contentPanel.loadURL(AppMapPlugin.getInstallGuideHTMLPath().toUri().toURL().toString());
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void onJavaScriptApplicationReady() {
        jcefBridge.addHandler(request -> {
            LOG.debug("postMessage received message: " + request);

            try {
                var json = gson.fromJson(request, JsonObject.class);
                var type = json.has("type") ? json.getAsJsonPrimitive("type").getAsString() : null;
                if (type != null) {
                    switch (type) {
                        case "postInitialize":
                            // ignored
                            break;

                        case "click-link":
                            if (json.has("uri")) {
                                openExternalLink(json.getAsJsonPrimitive("uri").getAsString());
                            }
                            break;

                        case "open-file":
                            ApplicationManager.getApplication().invokeLater(() -> {
                                var path = Paths.get(json.getAsJsonPrimitive("file").getAsString());
                                var file = LocalFileSystem.getInstance().findFileByNioFile(path);
                                if (file != null) {
                                    FileEditorManager.getInstance(project).openFile(file, true);
                                }
                            });
                            break;

                        case "openAppmap":
                            LOG.error("TODO");
                            break;

                        case "open-page":
                            var viewId = json.getAsJsonPrimitive("page").getAsString();
                            var viewProject = gson.fromJson(json.get("project"), ProjectMetadata.class);
                            var allProjects = findProjects();
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
                            break;

                        case "view-problems":
                            ApplicationManager.getApplication().invokeLater(() -> {
                                FindingsViewTab.activateFindingsTab(project);
                            });
                            break;

                        case "clipboard": {
                            // fixme send telemetry, as in VSCode?
                            var content = json.getAsJsonPrimitive("text").getAsString();
                            LOG.debug("Copying text to clipboard: " + content);

                            var target = new StringSelection(content);
                            ClipboardSynchronizer.getInstance().setContent(target, target);
                            break;
                        }

                        case "perform-install": {
                            var path = json.getAsJsonPrimitive("path").getAsString();
                            var language = json.getAsJsonPrimitive("language").getAsString();
                            executeInstallCommand(path, language);
                            break;
                        }

                        case "perform-auth": {
                            AppMapLoginAction.authenticate();
                            TelemetryService.getInstance().sendEvent("analysis:cta_interaction");
                            break;
                        }

                        case "generate-openapi": {
                            var projectPath = json.has("projectPath") ? json.getAsJsonPrimitive("projectPath").getAsString() : null;
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
                            break;
                        }

                        default:
                            LOG.warn("Unhandled message type: " + type);
                    }
                }
            } catch (Exception e) {
                LOG.warn("error handling command: " + request, e);
            }

            return new JBCefJSQuery.Response("Received " + request);
        });

        contentPanel.getCefBrowser().executeJavaScript(createCallbackJS(jcefBridge, "postMessage"), "", 0);

        postMessage(createInitMessageJSON());
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

    private void postMessage(@NotNull JsonElement json) {
        contentPanel.getCefBrowser().executeJavaScript("window.postMessage(" + gson.toJson(json) + ")", "", 0);
    }

    private @NotNull JsonObject createInitMessageJSON() {
        // this contains all necessary data for the components, calculated under progress
        var projects = findProjects();

        var json = createBasePropertiesMessage("init");
        json.add("projects", gson.toJsonTree(projects));
        json.add("disabledPages", new JsonArray());
        return json;
    }

    private @NotNull JsonObject createBasePropertiesMessage(@NotNull String messageType) {
        var settings = AppMapApplicationSettingsService.getInstance();
        var isAuthenticated = settings.getApiKey() != null;

        var json = new JsonObject();
        json.addProperty("type", messageType);
        json.addProperty("page", type.getPageId());
        json.addProperty("userAuthenticated", isAuthenticated);
        json.addProperty("analysisEnabled", settings.isAnalysisEnabled());
        json.addProperty("findingsEnabled", settings.isEnableFindings());
        return json;
    }

    private @NotNull JsonObject createPageNavigationJSON(@NotNull InstallGuideViewPage page) {
        var json = new JsonObject();
        json.addProperty("type", "page");
        json.addProperty("page", page.getPageId());
        return json;
    }

    private @NotNull JsonObject createUpdateProjectsMessage() {
        var json = new JsonObject();
        json.addProperty("type", "projects");
        json.add("projects", gson.toJsonTree(findProjects()));
        return json;
    }

    private @NotNull JsonObject createUpdateSettingsMessage() {
        // we're reusing the init properties to avoid duplicate code,
        // the "settings" handler of installGuideView.js is only applying supported properties
        var settings = createBasePropertiesMessage("settings");
        settings.add("projects", gson.toJsonTree(findProjects()));
        return settings;
    }

    @NotNull
    private List<ProjectMetadata> findProjects() {
        return ProjectDataService.getInstance(project).getAppMapProjects();
    }

    @NotNull
    private String createCallbackJS(JBCefJSQuery query, @NotNull String functionName) {
        return "if (!window.AppLand) window.AppLand={}; window.AppLand." + functionName + "=function(name) {" +
                query.inject("name") + "};";
    }

    @Override
    public @NotNull JComponent getComponent() {
        return contentPanel.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return AppMapBundle.get("installGuide.editor.title");
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public void dispose() {
        LOG.debug("Disposing AppLand install guide editor");
    }

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    public void openDevTools() {
        ApplicationManager.getApplication().invokeLater(contentPanel::openDevtools);
    }

    private boolean openExternalLink(@Nullable String url) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            navigating.set(true);
            BrowserUtil.browse(url);
            return true;
        }
        return false;
    }

    /**
     * RunProfileState, which is suitable for a terminal console with a PTY.
     * The default RunAnythingRunProfileState is not suitable, but we're following it for best practices.
     */
    private static class AppMapRunProfileState extends CommandLineState {
        private final GeneralCommandLine commandLine;

        protected AppMapRunProfileState(@NotNull GeneralCommandLine commandLine, @NotNull ExecutionEnvironment environment) {
            super(environment);
            this.commandLine = commandLine;
        }

        @Override
        public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
            var processHandler = startProcess();
            ProcessTerminatedListener.attach(processHandler);

            var console = new TerminalExecutionConsole(getEnvironment().getProject(), processHandler);
            console.attachToProcess(processHandler);
            return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
        }

        @NotNull
        @Override
        protected ProcessHandler startProcess() throws ExecutionException {
            // not a com.intellij.execution.process.KillableColoredProcessHandler,
            // because the console is handling colors
            return new KillableProcessHandler(commandLine) {
                @Override
                protected BaseOutputReader.@NotNull Options readerOptions() {
                    return new BaseOutputReader.Options() {
                        @Override
                        public boolean splitToLines() {
                            return false;
                        }

                        @Override
                        public BaseDataReader.SleepingPolicy policy() {
                            return BaseDataReader.SleepingPolicy.BLOCKING;
                        }
                    };
                }
            };
        }
    }
}
