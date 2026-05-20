package appland.cli;

import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value
public class ManifestAsset {
    public @NotNull String url;
    public @Nullable String digest;
}
