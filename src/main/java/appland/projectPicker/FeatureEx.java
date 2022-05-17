package appland.projectPicker;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class FeatureEx extends Feature {
    @Nullable String depFile;
    @Nullable String plugin;
    @Nullable String pluginType;
}
