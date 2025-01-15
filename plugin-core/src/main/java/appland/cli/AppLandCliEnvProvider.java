package appland.cli;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.Map;

/**
 * Extension to abstract the environment setup for CLI commands.
 */
public interface AppLandCliEnvProvider {
    ExtensionPointName<AppLandCliEnvProvider> EP_NAME = ExtensionPointName.create("appland.cli.envProvider");

    Map<String, String> getEnvironment();
}
