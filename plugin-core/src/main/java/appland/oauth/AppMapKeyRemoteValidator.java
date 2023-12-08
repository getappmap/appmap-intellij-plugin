package appland.oauth;

import appland.AppMapBundle;
import appland.AppMapPlugin;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Validates the key by making an HTTP request to the AppLand server.
 * <p>
 * Because validation requires sending an HTTP request, the input validation is performed in a background thread
 * under progress.
 */
class AppMapKeyRemoteValidator implements InputValidator {
    private final @NotNull Project project;

    AppMapKeyRemoteValidator(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public boolean checkInput(String inputString) {
        if (inputString.isEmpty()) {
            return false;
        }

        try {
            var title = AppMapBundle.get("action.appMapLoginByKey.progressTitle");
            var task = new Task.WithResult<Integer, Exception>(project, title, true) {
                @Override
                protected Integer compute(@NotNull ProgressIndicator indicator) throws Exception {
                    return makeRemoteRequest(inputString);
                }
            };
            task.queue();

            var statusCode = task.getResult();
            return statusCode < 300;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canClose(String inputString) {
        return checkInput(inputString);
    }

    @RequiresBackgroundThread
    private int makeRemoteRequest(@NotNull String key) throws IOException {
        try {
            return HttpRequests.request(AppMapPlugin.DEFAULT_SERVER_URL.resolve("/api/api_keys/check")).tuner(connection -> {
                        ((HttpURLConnection) connection).setRequestMethod("HEAD");
                        connection.setRequestProperty("Accept", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + key);
                    })
                    .tryConnect();
        } catch (HttpRequests.HttpStatusException e) {
            return e.getStatusCode();
        }
    }
}
