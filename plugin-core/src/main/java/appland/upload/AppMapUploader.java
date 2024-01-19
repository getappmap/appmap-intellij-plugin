package appland.upload;

import appland.AppMapBundle;
import appland.Icons;
import appland.settings.AppMapProjectSettingsService;
import com.google.common.html.HtmlEscapers;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationInfo;
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
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

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
                    urlConsumer.accept(confirmationURL(project, response));
                } catch (IOException e) {
                    LOG.warn("Uploading AppMap failed", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project,
                                AppMapBundle.get("upload.uploadFailed.message",
                                        file.getName(),
                                        HtmlEscapers.htmlEscaper().escape(e.getMessage())),
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
        return resolveSubPath(parsed, "/api/appmaps/create_upload").toExternalForm();
    }

    private static @NotNull String confirmationURL(@NotNull Project project, UploadResponse response) {
        var url = StringUtil.defaultIfEmpty(AppMapProjectSettingsService.getState(project).getCloudServerUrl(), DEFAULT_SERVER_URL);
        var parsed = Urls.parse(url, false);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid URL: " + url);
        }

        return resolveSubPath(parsed, "/scenario_uploads")
                .resolve(String.valueOf(response.id))
                .addParameters(Map.of("token", response.token))
                .toExternalForm();
    }

    /**
     * Helper method to resolve sub paths of Urls.
     * Newer SDKs insert a double-slash if a path leading with '/' is resolved, but older SDKs
     * don't properly resolve a path without a leading '/' if the current path is empty.
     */
    private static Url resolveSubPath(@NotNull Url url, @NotNull String subPath) {
        // Versions earlier than 2023.3 always need a leading /,
        // but 2023.3 resolves to two leading slashes if a leading slash is passed.
        var needsLeadingSlash = ApplicationInfo.getInstance().getBuild().getBaselineVersion() < 233;
        var fixedPath = needsLeadingSlash
                ? "/" + StringUtil.trimStart(subPath, "/")
                : StringUtil.trimStart(subPath, "/");
        return url.resolve(fixedPath);
    }
}
