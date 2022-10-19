package appland.validator;

import appland.AppMapBundle;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

public final class UrlInputValidator implements InputValidatorEx {
    public static final InputValidatorEx INSTANCE = new UrlInputValidator();

    @Override
    public boolean checkInput(@NlsSafe String inputString) {
        return getErrorText(inputString) == null;
    }

    @Override
    public boolean canClose(@NlsSafe String inputString) {
        return true;
    }

    @Override
    public @NlsContexts.DetailedDescription @Nullable String getErrorText(@NonNls String inputString) {
        if (inputString.isBlank()) {
            return AppMapBundle.get("urlValidation.empty");
        }

        try {
            var url = new URL(inputString);
            if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
                return AppMapBundle.get("urlValidation.protocolMissing");
            }

            if (url.getHost() == null || url.getHost().isBlank()) {
                return AppMapBundle.get("urlValidation.hostMissing");
            }

            return null;
        } catch (MalformedURLException e) {
            return AppMapBundle.get("urlValidation.parsingFailed");
        }
    }
}
