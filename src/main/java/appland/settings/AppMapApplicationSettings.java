package appland.settings;

import java.util.Objects;

/**
 * Persistent application state of the AppMap plugin.
 */
public class AppMapApplicationSettings {
    private volatile boolean appmapInstructionsViewed = false;
    private volatile boolean firstStart = true;

    public boolean isAppmapInstructionsViewed() {
        return appmapInstructionsViewed;
    }

    public void setAppmapInstructionsViewed(boolean appmapInstructionsViewed) {
        this.appmapInstructionsViewed = appmapInstructionsViewed;
    }

    public boolean isFirstStart() {
        return firstStart;
    }

    public void setFirstStart(boolean firstStart) {
        this.firstStart = firstStart;
    }

    @Override
    public String toString() {
        return "AppMapApplicationSettings{" +
                "appmapInstructionsViewed=" + appmapInstructionsViewed +
                ", firstStart=" + firstStart +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppMapApplicationSettings that = (AppMapApplicationSettings) o;
        return appmapInstructionsViewed == that.appmapInstructionsViewed && firstStart == that.firstStart;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appmapInstructionsViewed, firstStart);
    }
}
