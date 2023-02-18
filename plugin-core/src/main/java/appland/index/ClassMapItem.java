package appland.index;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class ClassMapItem {
    final @Nullable String parentId;
    final @NotNull String id;
    final @NotNull String name;
}