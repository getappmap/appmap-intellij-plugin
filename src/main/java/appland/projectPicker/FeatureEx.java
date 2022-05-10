package appland.projectPicker;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FeatureEx extends Feature {
    @Nullable String depFile;
    @Nullable String plugin;
    @Nullable String pluginType;
}
