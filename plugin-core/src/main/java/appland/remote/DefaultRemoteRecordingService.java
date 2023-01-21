package appland.remote;

import appland.files.AppMapFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
public class DefaultRemoteRecordingService implements RemoteRecordingService {
    static final String URL_SUFFIX = "/_appmap/record";
    private static final Logger LOG = Logger.getInstance("#appmap.remote");
    private static final int READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    private static final int CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);

    private final Gson GSON = new GsonBuilder().create();

    @Override
    public boolean isRecording(@NotNull String baseURL) {
        assertIsNotEDT();
        LOG.debug("Retrieving status of AppMap recording for base URL: " + baseURL);

        var request = setupRequest(HttpRequests.request(url(baseURL, URL_SUFFIX)));
        try {
            var responseData = request.readString();
            return GSON.fromJson(responseData, RecordGetResponse.class).enabled;
        } catch (Exception e) {
            LOG.debug("exception retrieving recording status", e);
            return false;
        }
    }

    @Override
    public boolean startRecording(@NotNull String baseURL) {
        assertIsNotEDT();
        LOG.debug("Starting AppMap recording for base URL: " + baseURL);

        var request = setupRequest(HttpRequests.post(url(baseURL, URL_SUFFIX), HttpRequests.JSON_CONTENT_TYPE));
        try {
            return request.tryConnect() == HttpStatus.SC_OK;
        } catch (Exception e) {
            LOG.debug("exception retrieving recording status", e);
            return false;
        }
    }

    @Override
    public @Nullable Path stopRecording(@NotNull String baseURL, @NotNull Path storageDirectoryPath, @NotNull String name) {
        assertIsNotEDT();
        assert ProgressManager.getInstance().hasProgressIndicator();

        var appMapFile = AppMapFiles.appMapFilename(storageDirectoryPath, name);
        LOG.debug("Stopping AppMap recording for base URL: " + baseURL + ", storing at %s", appMapFile);

        var request = setupRequest(HttpRequests.delete(url(baseURL, URL_SUFFIX), HttpRequests.JSON_CONTENT_TYPE));
        try {
            // stream to file on disk, it creates the necessary parent directories.
            // throws a HttpStatusException for response status >= 400
            request.saveToFile(appMapFile, ProgressManager.getGlobalProgressIndicator());

            // update metadata
            var updated = AppMapFiles.updateMetadata(appMapFile, name);
            return updated ? appMapFile : null;
        } catch (Exception e) {
            LOG.debug("exception retrieving recording status", e);
            return null;
        }
    }

    static RequestBuilder setupRequest(@NotNull RequestBuilder request) {
        request.gzip(true);
        request.productNameAsUserAgent();
        request.throwStatusCodeException(true);
        request.connectTimeout(CONNECT_TIMEOUT);
        request.readTimeout(READ_TIMEOUT);
        return request;
    }

    @NotNull
    static String url(@NotNull String baseURL, @NotNull String path) {
        return String.format("%s/%s", StringUtil.trimTrailing(baseURL, '/'), StringUtil.trimLeading(path, '/'));
    }

    static void assertIsNotEDT() {
        assert !ApplicationManager.getApplication().isDispatchThread();
    }
}
