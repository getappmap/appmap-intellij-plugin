package appland.notifications;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import appland.actions.StopAppMapRecordingAction;
import appland.settings.AppMapApplicationSettingsService;
import appland.settings.AppMapProjectConfigurable;
import appland.startup.FirstAppMapLaunchStartupActivity;
import appland.webviews.navie.NavieEditorProvider;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.EdtInvocationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

import static appland.AppMapBundle.lazy;

public final class AppMapNotifications {
    public static final String REMOTE_RECORDING_ID = "appmap.remoteRecording";
    public static final String TELEMETRY_ID = "appmap.telemetry";
    public static final String GENERIC_NOTIFICATIONS_ID = "appmap.generic";
    public static final String SETTINGS_NOTIFICATIONS_ID = "appmap.settings";

    public static void showExpiringRecordingNotification(@NotNull Project project,
                                                         @Nullable String title,
                                                         @NotNull String content,
                                                         @NotNull NotificationType type,
                                                         boolean withClose) {

        ApplicationManager.getApplication().invokeLater(() -> {
            // cast to Icon to avoid an ambiguity with 2021.2
            var notification = new Notification(REMOTE_RECORDING_ID, "", type);
            if (title != null) {
                notification.setTitle(title);
            }
            notification.setContent(content);
            notification.setListener(NotificationListener.URL_OPENING_LISTENER);

            if (withClose) {
                notification = notification.addAction(NotificationAction.create(
                        lazy("notification.closeButton"),
                        (e, n) -> n.expire()));
            }

            notification.notify(project);
        }, ModalityState.any());
    }

    public static void showExpandedRecordingNotification(@NotNull Project project,
                                                         @Nullable String title,
                                                         @NotNull String content,
                                                         @NotNull NotificationType type,
                                                         boolean withClose,
                                                         boolean withStopAction,
                                                         boolean withHelpLink) {

        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new AppMapFullContentNotification(
                    REMOTE_RECORDING_ID, null,
                    title, null, content,
                    type, NotificationListener.URL_OPENING_LISTENER
            );

            if (withClose) {
                var closeAction = NotificationAction.create(lazy("notification.closeButton"),
                        (e, n) -> n.expire());
                notification = notification.addAction(closeAction);
            }

            if (withStopAction) {
                notification = notification.addAction(NotificationAction.create(
                        lazy("notification.stopButton"),
                        (e, n) -> {
                            n.expire();
                            new StopAppMapRecordingAction().actionPerformed(e);
                        }));
            }

            if (withHelpLink) {
                notification = notification.addAction(NotificationAction.create(
                        lazy("notification.recordingHelpButton"),
                        (e, n) -> {
                            n.expire();
                            BrowserUtil.browse(AppMapPlugin.REMOTE_RECORDING_HELP_URL);
                        }));
            }

            notification.notify(project);
        }, ModalityState.any());
    }

    public static void showTelemetryNotification(@NotNull Project project,
                                                 @Nullable String title,
                                                 @NotNull String content,
                                                 @NotNull NotificationType type,
                                                 @NotNull Consumer<Boolean> onDismiss) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new AppMapFullContentNotification(
                    TELEMETRY_ID, null,
                    title, null, content,
                    type, null
            );

            var denyAction = NotificationAction.create(lazy("telemetry.permission.deny"), (e, n) -> {
                onDismiss.accept(false);
                n.expire();
            });

            notification = notification.addAction(denyAction);
            notification.notify(project);
        });
    }

    public static void showFirstAppMapNotification(@NotNull Project project, @NotNull VirtualFile appMap) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var content = AppMapBundle.get("notification.firstAppMap.content");

            Notification notification = new AppMapFullContentNotification(
                    GENERIC_NOTIFICATIONS_ID, null,
                    null, null, content,
                    NotificationType.INFORMATION, null
            );

            var openAction = NotificationAction.create(lazy("notification.firstAppMap.openAction"), (e, n) -> {
                NavieEditorProvider.openEditor(project, DataContext.EMPTY_CONTEXT);
                FirstAppMapLaunchStartupActivity.showAppMapToolWindow(project);
                n.expire();
            });

            notification = notification.addAction(openAction);
            notification.notify(project);
        });
    }

    public static void showSignInNotification(@NotNull Project project) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var notification = new AppMapFullContentNotification(
                    GENERIC_NOTIFICATIONS_ID, null,
                    null, null, AppMapBundle.get("notification.appMapSignIn.content"),
                    NotificationType.INFORMATION, null
            );
            notification.notify(project);
        });
    }

    public static void showNavieUnavailableNotification(@NotNull Project project) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var notification = new AppMapFullContentNotification(
                    GENERIC_NOTIFICATIONS_ID, null,
                    null, null, AppMapBundle.get("notification.navieUnavailable.content"),
                    NotificationType.INFORMATION, null
            );
            notification.notify(project);
        });
    }

    @SuppressWarnings({"removal", "DialogTitleCapitalization"})
    public static void showWebviewProxyBrokenWarning(@NotNull Project project) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            Messages.showDialog(
                    project,
                    AppMapBundle.get("notification.brokenProxySupport.content"),
                    AppMapBundle.get("notification.brokenProxySupport.title"),
                    new String[]{AppMapBundle.get("notification.brokenProxySupport.confirmAction")},
                    0,
                    Messages.getWarningIcon(),
                    new DialogWrapper.DoNotAskOption.Adapter() {
                        @Override
                        public void rememberChoice(boolean isSelected, int exitCode) {
                            AppMapApplicationSettingsService.getInstance().setShowBrokenProxyWarning(!isSelected);
                        }
                    }
            );
        });
    }

    /**
     * @return {@code true} if the warning about broken proxy support of webview should be displayed.
     * The warning is only shown for 2024.1 and for users which have an HTTP proxy configured.
     */
    public static boolean isWebviewProxyWarningRequired() {
        var proxySettings = HttpConfigurable.getInstance();
        return ApplicationInfo.getInstance().getBuild().getBaselineVersion() == 241
                && proxySettings.USE_HTTP_PROXY && !proxySettings.PROXY_TYPE_IS_SOCKS
                && AppMapApplicationSettingsService.getInstance().isShowBrokenProxyWarning();
    }

    public static void showAppMapJsonExportFailedNotification(@NotNull Project project, @NotNull String error) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var notification = new AppMapFullContentNotification(
                    GENERIC_NOTIFICATIONS_ID, null,
                    null, null, AppMapBundle.get("notification.exportAppMapJson.content", error),
                    NotificationType.ERROR, null
            );
            notification.notify(project);
        });
    }

    public static void showReloadProjectNotification(@NotNull Project project) {
        var content = AppMapBundle.get("notification.reloadProject.content");

        // don't show again if the reload notification is already displayed
        var manager = NotificationsManager.getNotificationsManager();
        var appMapNotifications = manager.getNotificationsOfType(AppMapFullContentNotification.class, project);
        var isReloadNotificationShown = Arrays.stream(appMapNotifications).anyMatch(notification -> {
            return content.equals(notification.getContent())
                    && !notification.isExpired()
                    && notification.getBalloon() != null;
        });
        if (isReloadNotificationShown) {
            return;
        }

        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var notification = new AppMapFullContentNotification(
                    SETTINGS_NOTIFICATIONS_ID, null,
                    null, null,
                    content,
                    NotificationType.INFORMATION, null
            );
            notification.notify(project);
        });
    }

    /**
     * @return {@code true} if the current IDE has broken text input.
     */
    public static boolean isWebviewTextInputBroken() {
        var buildBaseline = ApplicationInfo.getInstance().getBuild().getBaselineVersion();

        // Linux <= 2023.1 is broken,
        // https://youtrack.jetbrains.com/issue/JBR-5348/JCEF-OSR-Keyboard-doesnt-work-on-Linux
        return SystemInfo.isLinux && buildBaseline <= 231;
    }

    /**
     * @param project  Current project
     * @param forNavie If the notification is shown for the Navie webview {@code true} of for the sign-in webview {@code false}.
     */
    @SuppressWarnings("removal")
    public static void showWebviewTextInputBrokenMessage(@NotNull Project project, boolean forNavie) {
        // don't show in our unit tests because there's no user interaction
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        var properties = PropertiesComponent.getInstance();
        var hideMessagePropertyKey = forNavie ? "appmap.navie.hideIsBrokenMessage" : "appmap.signin.hideIsBrokenMessage";
        if (properties.getBoolean(hideMessagePropertyKey, false)) {
            return;
        }

        var titleKey = forNavie ? "webview.navie.webviewBroken.title" : "webview.signin.webviewBroken.title";
        var messageKey = forNavie ? "webview.navie.webviewBroken.message" : "webview.signin.webviewBroken.message";

        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showDialog(project,
                    AppMapBundle.get(messageKey),
                    AppMapBundle.get(titleKey),
                    new String[]{Messages.getOkButton()},
                    0,
                    Messages.getWarningIcon(),
                    // already deprecated in 2022.1, but there's no alternative API in Messages.
                    new DialogWrapper.DoNotAskOption.Adapter() {
                        @Override
                        public void rememberChoice(boolean isSelected, int exitCode) {
                            properties.setValue(hideMessagePropertyKey, isSelected, false);
                        }

                        @Override
                        public @NotNull String getDoNotShowMessage() {
                            return AppMapBundle.get("notification.dontShowAgainOption");
                        }
                    });
        }, ModalityState.defaultModalityState());
    }

    public static void showNaviePinnedFileTooLargeNotification(@NotNull Project project,
                                                               int skippedFiles,
                                                               int fileSizeLimitKB) {
        EdtInvocationManager.invokeLaterIfNeeded(() -> {
            var notification = new AppMapFullContentNotification(
                    GENERIC_NOTIFICATIONS_ID, null,
                    AppMapBundle.get("notification.naviePinnedFileTooLarge.title"),
                    null,
                    AppMapBundle.get("notification.naviePinnedFileTooLarge.content", skippedFiles, fileSizeLimitKB),
                    NotificationType.INFORMATION, null
            );

            var action = AppMapBundle.get("notification.naviePinnedFileTooLarge.showSettings");
            notification.addAction(new NotificationAction(action) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    notification.expire();
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, AppMapProjectConfigurable.class);
                }
            });
            notification.notify(project);
        });
    }

    /**
     * Displays a notification that the Copilot authentication is required in all open projects.
     */
    public static void showFirstCopilotIntegrationEnabled() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        ReadAction.run(() -> {
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                new AppMapFullContentNotification(
                        GENERIC_NOTIFICATIONS_ID, null,
                        AppMapBundle.get("notification.copilotIntegrationAvailable.title"),
                        null,
                        AppMapBundle.get("notification.copilotIntegrationAvailable.content"),
                        NotificationType.INFORMATION, null
                ).notify(project);
            }
        });
    }

    /**
     * Displays a notification that the Copilot authentication is required in all open projects.
     */
    public static void showCopilotAuthenticationRequired() {
        ApplicationManager.getApplication().assertIsDispatchThread();

        ReadAction.run(() -> {
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                new AppMapFullContentNotification(
                        GENERIC_NOTIFICATIONS_ID, null,
                        AppMapBundle.get("notification.copilotAuthRequired.title"),
                        null,
                        AppMapBundle.get("notification.copilotAuthRequired.content"),
                        NotificationType.WARNING, null
                ).notify(project);
            }
        });
    }
}
