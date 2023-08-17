package appland.index;

import appland.problemsView.model.TestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

/**
 * The basic metadata of an AppMap, extracted from metadata.json.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasicAppMapMetadata {
    @Nullable String name = null;
    @Nullable TestStatus testStatus = null;
}
