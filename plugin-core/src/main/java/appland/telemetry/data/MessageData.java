package appland.telemetry.data;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.SerializedName;

public class MessageData {
    @SerializedName("baseType")
    @NotNull final String baseType = "EventData";

    @SerializedName("baseData")
    @NotNull BaseData baseData;

    public MessageData(@NotNull BaseData baseData) {
        this.baseData = baseData;
    }
}
