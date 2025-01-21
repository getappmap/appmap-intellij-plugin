package appland.copilotChat.copilot;

import appland.AppLandTestExecutionPolicy;
import appland.AppMapBaseTest;
import appland.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class CopilotModelDefinitionTest extends AppMapBaseTest {
    @Test
    public void parseModelJson() throws IOException {
        var jsonFilePath = Path.of(AppLandTestExecutionPolicy.findAppMapHomePath())
                .resolve(Path.of("copilot", "model_2025-01-21.json"));
        var json = Files.readString(jsonFilePath, StandardCharsets.UTF_8);

        // must parse without exceptions
        var modelData = GsonUtils.GSON.fromJson(json, ModelsResponse.class);

        var gpt40Mini = Arrays.stream(modelData.models()).filter(model -> {
            return "gpt-4o-mini".equals(model.id());
        }).findFirst().orElse(null);
        assertNotNull(gpt40Mini);
        assertEquals(4096, gpt40Mini.capabilities().limits().maxOutputTokens());
        assertEquals(64000, gpt40Mini.capabilities().limits().maxPromptTokens());
    }

    private record ModelsResponse(@SerializedName("data") CopilotModelDefinition[] models) {
    }
}