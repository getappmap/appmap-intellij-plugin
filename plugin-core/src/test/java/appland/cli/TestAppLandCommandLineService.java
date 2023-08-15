package appland.cli;

public class TestAppLandCommandLineService extends DefaultCommandLineService {
    public static TestAppLandCommandLineService getInstance() {
        return (TestAppLandCommandLineService) AppLandCommandLineService.getInstance();
    }

    public String getDebugInfo() {
        return processes.toString();
    }
}
