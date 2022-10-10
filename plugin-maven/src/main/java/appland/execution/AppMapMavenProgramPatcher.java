package appland.execution;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;

import java.nio.file.Path;

public class AppMapMavenProgramPatcher extends AbstractAppMapJavaProgramPatcher {
    @Override
    protected boolean isSupported(@NotNull RunProfile configuration) {
        return configuration instanceof MavenRunConfiguration;
    }

    @Override
    protected @Nullable Path findAppMapOutputDirectory(@NotNull RunProfile configuration,
                                                       @Nullable VirtualFile workingDirectory) {
        return workingDirectory == null
                ? null
                : workingDirectory.toNioPath().resolve("target").resolve("appmap");
    }
}
