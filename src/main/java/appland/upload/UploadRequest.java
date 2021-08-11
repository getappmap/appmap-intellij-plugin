package appland.upload;

import com.google.gson.annotations.SerializedName;

class UploadRequest {
    @SerializedName("data")
    String appMapContent;

    UploadRequest(String appMapContent) {
        this.appMapContent = appMapContent;
    }
}
