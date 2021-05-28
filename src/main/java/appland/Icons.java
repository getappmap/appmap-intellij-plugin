package appland;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.PlatformIcons;

import javax.swing.*;

public class Icons {
    // 16x16
    public static final Icon APPMAP_FILE = IconLoader.getIcon("/icons/appmap.svg", Icons.class);
    public static final Icon APPMAP_DOCS = APPMAP_FILE;
    public static final Icon START_RECORDING_ACTION = AllIcons.Actions.Run_anything;
    public static final Icon STOP_RECORDING_ACTION = AllIcons.Actions.Suspend;

    // 13x13
    public static final Icon APPMAP_FILE_SMALL = IconLoader.getIcon("/icons/appmap_small.svg", Icons.class);
    public static final Icon TOOL_WINDOW = APPMAP_FILE_SMALL;
}
