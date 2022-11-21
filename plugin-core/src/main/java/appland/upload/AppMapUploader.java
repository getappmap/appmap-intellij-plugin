package appland.upload;

import appland.AppMapBundle;
import appland.Icons;
import appland.settings.AppMapProjectSettingsService;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

/**
 * Manages the uploading of AppMap files to the AppLand cloud servers.
 */
public class AppMapUploader {
    public static final String DEFAULT_SERVER_URL = "https://app.land";
    private static final Logger LOG = Logger.getInstance("#appmap.upload");

    /**
     * Upload the file. Asks the user for confirmation if the user did not confirm yet.
     * <p>
     * Must be called on the EDT.
     *
     * @param project The current project
     * @param file    The file to upload
     */
    @SuppressWarnings("DialogTitleCapitalization")
    public static void uploadAppMap(@NotNull Project project, @NotNull VirtualFile file, @NotNull Consumer<String> urlConsumer) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (AppMapProjectSettingsService.getState(project).isConfirmAppMapUpload()) {
            var reply = Messages.showOkCancelDialog(project,
                    AppMapBundle.get("upload.confirmation.message"),
                    AppMapBundle.get("upload.confirmation.title"),
                    AppMapBundle.get("upload.confirmation.ok"),
                    AppMapBundle.get("upload.confirmation.cancel"),
                    Icons.APPMAP_FILE, new DialogWrapper.DoNotAskOption.Adapter() {
                        @Override
                        public void rememberChoice(boolean isSelected, int exitCode) {
                            AppMapProjectSettingsService.getState(project).setConfirmAppMapUpload(!isSelected);
                        }
                    });

            if (reply != Messages.OK) {
                return;
            }
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, AppMapBundle.get("upload.progress.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    var document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> {
                        return FileDocumentManager.getInstance().getDocument(file);
                    });

                    if (document == null) {
                        LOG.warn("unable to load content of VirtualFile, file: " + file.getPath() + ", size: " + file.getLength());
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(project,
                                    AppMapBundle.get("upload.docUnavailable.message"),
                                    AppMapBundle.get("upload.docUnavailable.title"));
                        });
                        return;
                    }

                    var content = ApplicationManager
                            .getApplication()
                            .runReadAction((Computable<CharSequence>) document::getImmutableCharSequence);

                    var gson = new GsonBuilder().create();

                    var request = HttpRequests.post(uploadURL(project), "application/json")
                            .gzip(true)
                            .tuner(connection -> connection.setRequestProperty("X-Requested-With", "IntelliJUploader"));

                    var responseBody = request.connect(req -> {
                        req.write(gson.toJson(new UploadRequest(content.toString())));
                        return req.readString(ProgressManager.getGlobalProgressIndicator());
                    });

                    var response = gson.fromJson(responseBody, UploadResponse.class);
                    urlConsumer.consume(confirmationURL(project, response));
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

    @NotNull
    private static String uploadURL(@NotNull Project project) {
        var url = StringUtil.defaultIfEmpty(AppMapProjectSettingsService.getState(project).getCloudServerUrl(), DEFAULT_SERVER_URL);
        var parsed = Urls.parse(url, false);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid URL: " + url);
        }
        return parsed.resolve("/api/appmaps/create_upload").toExternalForm();
    }

    private static @NotNull String confirmationURL(@NotNull Project project, UploadResponse response) {
        var url = StringUtil.defaultIfEmpty(AppMapProjectSettingsService.getState(project).getCloudServerUrl(), DEFAULT_SERVER_URL);
        var parsed = Urls.parse(url, false);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid URL: " + url);
        }

        return parsed.resolve("/scenario_uploads")
                .resolve(String.valueOf(response.id))
                .addParameters(Map.of("token", response.token))
                .toExternalForm();
    }
}
