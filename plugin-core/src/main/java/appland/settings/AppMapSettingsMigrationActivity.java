package appland.settings;

import appland.rpcService.AppLandJsonRpcService;
import appland.startup.AppLandStartupActivity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppMapSettingsMigrationActivity extends AppLandStartupActivity {
    private final AtomicBoolean migratedApplicationSettings = new AtomicBoolean(false);

    @Override
    public void runActivity(@NotNull Project project) {
        if (migratedApplicationSettings.compareAndExchange(false, true)) {
            migrateApplicationSettings();
        }
    }

    static void migrateApplicationSettings() {
        var secureSettings = AppMapSecureApplicationSettingsService.getInstance();
        var settings = AppMapApplicationSettingsService.getInstance();
        var cliEnvironment = new HashMap<>(settings.getCliEnvironment());

        // Migrate OPENAI_API_KEY to our secure model config settings,
        // https://github.com/getappmap/appmap-intellij-plugin/issues/874
        var openAIKey = StringUtil.defaultIfEmpty(
                secureSettings.getOpenAIKey(),
                cliEnvironment.get(AppLandJsonRpcService.OPENAI_API_KEY)
        );
        if (openAIKey != null && !secureSettings.getModelConfig().containsKey("OPENAI_API_KEY")) {
            secureSettings.setModelConfigItem("OPENAI_API_KEY", openAIKey);

            secureSettings.setOpenAIKey(null);

            cliEnvironment.remove(AppLandJsonRpcService.OPENAI_API_KEY);
            settings.setCliEnvironment(cliEnvironment);
        }
    }
}
