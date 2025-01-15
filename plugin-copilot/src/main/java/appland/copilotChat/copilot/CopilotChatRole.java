package appland.copilotChat.copilot;

import com.google.gson.annotations.SerializedName;

/**
 * The roles of Copilot are not the same as of OpenAI.
 * If an unsupported role is passed, then /chat/completions returns a 400/Bad Request error.
 */
public enum CopilotChatRole {
    @SerializedName("user")
    User,
    @SerializedName("system")
    System,
    @SerializedName("assistant")
    Assistant
}
