package appland.javaAgent;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

interface Release {
    /**
     * When downloading the Java agent, two separate hosts may be queried: one
     * to get the tag of the latest version, and a (possibly) different host to
     * actually
     * download the JAR file. This method returns the latter.
     * @return the host download host
     */
    String getDownloadHost();

    List<Asset> getLatest(@NotNull ProgressIndicator progressIndicator) throws IOException;

    @Value
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    class Asset {
        @SerializedName("name")
        String fileName;
        @SerializedName("content_type")
        String contentType;
        @SerializedName("browser_download_url")
        String downloadUrl;

        void download(@NotNull ProgressIndicator progressIndicator, @NotNull Path targetFilePath) throws IOException {
            Objects.requireNonNull(downloadUrl);
            HttpRequests.request(downloadUrl).saveToFile(targetFilePath, progressIndicator);
        }
    }
}

