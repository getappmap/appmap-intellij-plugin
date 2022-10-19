package appland.upload;

import com.google.gson.annotations.SerializedName;

class UploadResponse {
    @SerializedName("id")
    Integer id;
    @SerializedName("token")
    String token;
}
