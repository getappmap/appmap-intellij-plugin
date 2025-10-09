package appland.cli;

import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class SemVerComparator implements Comparator<@Nullable SemVer> {
    public static final SemVerComparator INSTANCE = new SemVerComparator();

    @Override
    public int compare(@Nullable SemVer o1, @Nullable SemVer o2) {
        // sort null to the end
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;

        return o1.compareTo(o2);
    }
}
