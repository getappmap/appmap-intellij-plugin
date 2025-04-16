package appland.rpcService;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public record NavieThreadQueryV1Response(
        List<NavieThread> result
) {
  public record NavieThread(
          @SerializedName("id") String id,
          @SerializedName("path") String path,
          @SerializedName("title") @Nullable String title,
          @SerializedName("created_at") String createdAt,
          @SerializedName("updated_at") String updatedAt
  ) {
  }
}
