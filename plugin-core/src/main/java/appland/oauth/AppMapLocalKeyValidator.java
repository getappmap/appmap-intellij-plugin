package appland.oauth;

import appland.AppMapBundle;
import com.intellij.openapi.ui.InputValidatorEx;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

class AppMapLocalKeyValidator implements InputValidatorEx {
    // email followed by the license id
    private static final Pattern DECODED_VALUE_PATTERN = Pattern.compile("^.+@.+:.+$");

    @Override
    public boolean canClose(String inputString) {
        // allow closing even if the local validation returned false
        // to always delegate to the remote validation endpoint as final validation
        return true;
    }

    @Override
    public boolean checkInput(String inputString) {
        // allow closing even if the local validation returned false
        // to always delegate to the remote validation endpoint as final validation
        return true;
    }

    @Override
    public @Nullable String getErrorText(@NonNls String inputString) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(inputString.trim());
        } catch (IllegalArgumentException e) {
            decoded = null;
        }

        var isValid = decoded != null
                && DECODED_VALUE_PATTERN.asMatchPredicate().test(new String(decoded, StandardCharsets.UTF_8));
        return isValid ? null : AppMapBundle.get("action.appMapLoginByKey.dialog.validation.invalidKeyError");
    }
}
