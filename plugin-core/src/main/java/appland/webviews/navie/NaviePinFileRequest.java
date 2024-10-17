package appland.webviews.navie;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

/**
 * @param name    The name of the file being pinned
 * @param uri     URI of the file to add
 * @param content The content for the file
 */
public record NaviePinFileRequest(@SerializedName("name") @NotNull String name,
                                  @SerializedName("uri") @NotNull String uri,
                                  @SerializedName("content") @NotNull String content) {

}
