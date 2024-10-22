package appland.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

@TestOnly
public class TestCommandLineService extends DefaultCommandLineService {
    /**
     * We're exposing this to allow tests to access fields of DefaultCommandLineService.
     */
    public static @NotNull TestCommandLineService getInstance() {
        return (TestCommandLineService) AppLandCommandLineService.getInstance();
    }
}
