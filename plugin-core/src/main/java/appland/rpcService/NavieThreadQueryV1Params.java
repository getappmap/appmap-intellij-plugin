package appland.rpcService;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record NavieThreadQueryV1Params(
        @SerializedName("threadId") @Nullable String threadId,
        @SerializedName("maxCreatedAt") @Nullable String maxCreatedAt,
        @SerializedName("orderBy") @Nullable String orderBy,
        @SerializedName("limit") @Nullable Integer limit,
        @SerializedName("offset") @Nullable Integer offset,
        @SerializedName("projectDirectories") @Nullable String[] projectDirectories
) {
  
}
