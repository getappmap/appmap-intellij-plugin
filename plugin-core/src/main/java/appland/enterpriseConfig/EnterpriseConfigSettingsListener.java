package appland.enterpriseConfig;

import appland.settings.AppMapSettingsListener;

public class EnterpriseConfigSettingsListener implements AppMapSettingsListener {
    @Override
    public void configurationUrlChanged() {
        EnterpriseConfigService.getInstance().applyAsync();
    }
}
