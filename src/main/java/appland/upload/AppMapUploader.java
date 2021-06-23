package appland.upload;

import appland.AppMapBundle;
import appland.Icons;
import appland.settings.AppMapProjectSettingsService;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.HttpRequests;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Manages the uploading of AppMap files to the AppLand cloud servers.
 */
public class AppMapUploader {
    private static final Logger LOG = Logger.getInstance("#appmap.upload");
    private static final String SERVER_URL = "https://app.land";
    private static final String UPLOAD_URL = SERVER_URL + "/api/appmaps/create_upload";
    private static final String CONFIRMATION_URL_BASE = SERVER_URL + "/scenario_uploads";

    /**
     * Upload the file. Asks the user for confirmation if the user did not confirm yet.
     * <p>
     * Must be called on the EDT.
     *
     * @param project The current project
     * @param file    The file to upload
     */
    @SuppressWarnings("DialogTitleCapitalization")
    public static void uploadAppMap(@NotNull Project project, @NotNull VirtualFile file) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (!AppMapProjectSettingsService.getState(project).isAppMapUploadConfirmed()) {
            var reply = Messages.showYesNoDialog(AppMapBundle.get("upload.confirmation.message"),
                    AppMapBundle.get("upload.confirmation.title"),
                    Icons.APPMAP_FILE);
            if (reply == Messages.NO) {
                return;
            }

            AppMapProjectSettingsService.getState(project).setAppMapUploadConfirmed(true);
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, AppMapBundle.get("upload.progress.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    var request = HttpRequests.post(UPLOAD_URL, "application/json")
                            .gzip(true)
                            .forceHttps(true)
                            .tuner(connection -> connection.setRequestProperty("X-Requested-With", "IntelliJUploader"));
                    request.write(file.contentsToByteArray());

                    var replyContent = request.readString(ProgressManager.getGlobalProgressIndicator());
                    var reply = new GsonBuilder().create().fromJson(replyContent, UploadResponse.class);

                    var confirmationURL = CONFIRMATION_URL_BASE + "/" + reply.id + "?token=" + reply.token;
                    ApplicationManager.getApplication().invokeLater(() -> BrowserUtil.browse(confirmationURL));
                } catch (IOException e) {
                    LOG.warn("Uploading AppMap failed", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project,
                                AppMapBundle.get("upload.uploadFailed.message", file.getName(), StringEscapeUtils.escapeHtml(e.getMessage())),
                                AppMapBundle.get("upload.uploadFailed.title"));
                    });
                }
            }
        });
    }
}
