package appland;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class Icons {
    // 16x16
    public static final Icon APPMAP_FILE = IconLoader.getIcon("/icons/appmap.svg", Icons.class);
    public static final Icon APPMAP_DOCS = APPMAP_FILE;

    // 13x13
    public static final Icon APPMAP_FILE_SMALL = IconLoader.getIcon("/icons/appmap_small.svg", Icons.class);
    public static final Icon TOOL_WINDOW = APPMAP_FILE_SMALL;
}
