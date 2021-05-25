package appland.settings;

import java.util.Objects;

/**
 * Persistent application state of the AppMap plugin.
 */
public class AppMapApplicationSettings {
    private volatile boolean appmapInstructionsViewed = false;

    public boolean isAppmapInstructionsViewed() {
        return appmapInstructionsViewed;
    }

    public void setAppmapInstructionsViewed(boolean appmapInstructionsViewed) {
        this.appmapInstructionsViewed = appmapInstructionsViewed;
    }

    @Override
    public String toString() {
        return "AppMapLocalApplicationState{" +
                "appmapInstructionsViewed=" + appmapInstructionsViewed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapApplicationSettings that = (AppMapApplicationSettings) o;
        return appmapInstructionsViewed == that.appmapInstructionsViewed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appmapInstructionsViewed);
    }
}
