package appland.actions;

import appland.AppMapBundle;
import appland.Icons;
import appland.files.AppMapFiles;
import appland.files.AppMapVfsUtils;
import appland.notifications.AppMapNotifications;
import appland.remote.*;
import appland.settings.AppMapProjectSettingsService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class StopAppMapRecordingAction extends AnAction implements DumbAware, UpdateInBackground {
    private static final Logger LOG = Logger.getInstance(StopAppMapRecordingAction.class);

    public StopAppMapRecordingAction() {
        super(Icons.STOP_RECORDING_ACTION);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        var project = e.getProject();
        if (project == null) {
            return;
        }

        // don't hide the "Stop recording" action in the global list, only in the toolbar
        // we still want to allow a user to stop recording at any URL
        if (e.isFromActionToolbar()) {
            var recording = RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL() != null;
            e.getPresentation().setEnabledAndVisible(recording);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        new Task.Modal(project, AppMapBundle.get("action.stopAppMapRemoteRecording.locationProgress.title"), true) {
            private final AtomicReference<Path> location = new AtomicReference<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }

                indicator.setText(AppMapBundle.get("action.stopAppMapRemoteRecording.locationProgress.progressTitle"));
                location.set(findDefaultStorageLocation(project));
            }

            @Override
            public void onSuccess() {
                if (project.isDisposed()) {
                    return;
                }

                var locationResult = location.get();
                if (locationResult == null) {
                    LOG.warn("unable to locate the storage location for AppMaps");
                    return;
                }

                var form = StopRemoteRecordingDialog.show(project,
                        RemoteRecordingStatusService.getInstance(project).getActiveRecordingURL(),
                        AppMapProjectSettingsService.getState(project).getRecentRemoteRecordingURLs());
                if (form != null) {
                    stopAndSaveRemoteRecording(project, form, locationResult);
                }
            }
        }.queue();
    }

    private static void stopAndSaveRemoteRecording(@NotNull Project project,
                                                   @NotNull StopRemoteRecordingForm form,
                                                   @NotNull Path storageLocation) {
        new Task.Backgroundable(project, AppMapBundle.get("action.stopAppMapRemoteRecording.progressTitle"), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                var newFile = RemoteRecordingService.getInstance().stopRecording(form.getUrl(), storageLocation, form.getName());
                RemoteRecordingStatusService.getInstance(project).recordingStopped(form.getUrl());

                if (newFile != null && Files.exists(newFile)) {
                    var newVfsFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(newFile);
                    if (newVfsFile != null) {
                        // open the new file in an editor
                        ApplicationManager.getApplication().invokeLater(() -> {
                            FileEditorManager.getInstance(project).openFile(newVfsFile, true, true);
                        }, ModalityState.defaultModalityState());
                    }
                } else {
                    showStopRecordingFailedError(project, form.getUrl());
                }
            }
        }.queue();
    }

    /**
     * Try to find a reasonable default for the storage location.
     */
    @RequiresBackgroundThread
    protected static @Nullable Path findDefaultStorageLocation(@NotNull Project project) {
        // use storage location from a appmap.yml file, if available
        var configLocation = findConfiguredStorageLocation(project);
        if (configLocation != null) {
            return configLocation;
        }

        // fall back to "<project dir>/target/appmap/remote"
        var projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null) {
            // we can't use the NIO path, because we have to support temp Vfs filesystems in tests
            return AppMapVfsUtils.asNativePath(projectDir).resolve(Paths.get("target", "appmap", "remote"));
        }

        return null;
    }

    /**
     * @param project Current project
     * @return The first found and valid storage location, which is configured in a appmap.yml file of the project
     */
    @RequiresBackgroundThread
    private static @Nullable Path findConfiguredStorageLocation(@NotNull Project project) {
        return ReadAction.compute(() -> AppMapFiles.findAppMapConfigFiles(project)
                .stream()
                .map(StopAppMapRecordingAction::findAppMapDirectory)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
    }

    @RequiresReadLock
    private static @Nullable Path findAppMapDirectory(@NotNull VirtualFile appMapConfig) {
        if (appMapConfig.isInLocalFileSystem()) {
            var appMapDir = AppMapFiles.readAppMapDirConfigValue(appMapConfig);
            if (appMapDir != null) {
                try {
                    var nativeParentPath = AppMapVfsUtils.asNativePath(appMapConfig.getParent());
                    return nativeParentPath.resolve(appMapDir).resolve("remote");
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

    private static void showStopRecordingFailedError(@NotNull Project project, @NotNull String url) {
        AppMapNotifications.showExpandedRecordingNotification(project,
                AppMapBundle.get("notification.recordingStopFailed.title"),
                AppMapBundle.get("notification.recordingStopFailed.content", url),
                NotificationType.ERROR, true, false, true);
    }
}
