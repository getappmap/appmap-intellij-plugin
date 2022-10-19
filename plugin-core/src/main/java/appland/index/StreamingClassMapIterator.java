package appland.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.JsonReaderEx;

/**
 * Iterate a classMap.json file with GSON's streaming parser to reduce memory usage of the indexer.
 */
abstract class StreamingClassMapIterator {
    private static final Logger LOG = Logger.getInstance(StreamingClassMapIterator.class);

    protected abstract void onItem(int level, @NotNull String id, ClassMapItemType type, @NotNull String name);

    public void parse(@NotNull CharSequence content) {
        if (content.length() == 0) {
            return;
        }

        try (var json = new JsonReaderEx(content)) {
            parseArray(0, json, "", ClassMapItemType.ROOT);
        }
    }

    private void parseArray(int level, @NotNull JsonReaderEx json, @NotNull String parentId, @NotNull ClassMapItemType parentType) {
        json.beginArray();

        while (json.hasNext()) {
            var childrenReader = json.createSubReaderAndSkipValue();
            assert childrenReader != null;

            parseItem(level, parentId, parentType, childrenReader);
        }

        json.endArray();
    }

    private void parseItem(int level, @NotNull String parentId, @NotNull ClassMapItemType parentType, JsonReaderEx json) {
        json.beginObject();

        String name = null;
        String typeName = null;
        Boolean isStatic = null;
        JsonReaderEx children = null;

        while (true) {
            var property = json.nextNameOrNull();
            if (property == null) {
                break;
            }

            switch (property) {
                case "name":
                    name = json.nextString();
                    break;
                case "type":
                    typeName = json.nextString();
                    break;
                case "static":
                    isStatic = json.nextBoolean();
                    break;
                case "children":
                    children = json.createSubReaderAndSkipValue();
                    break;
                case "labels":
                    // array property, skip
                    json.skipValue();
                    break;
                default:
                    json.skipValue();
                    LOG.debug("Skipping classMap property: " + property);
                    break;
            }
        }

        if (name != null && typeName != null) {
            var type = ClassMapItemType.findByName(typeName);
            var separator = findItemSeparator(parentType, isStatic, type);

            // Some code object entries have a path-delimited package name,
            // but we want each package name token to be its own object.
            String itemPath;
            int childLevel = level;
            if (type == ClassMapItemType.Package) {
                itemPath = parentId;
                for (var parentName : StringUtil.split(name, "/")) {
                    itemPath = joinPath("/", itemPath, parentName);
                    onItem(childLevel, typeName + ":" + itemPath, type, name);
                    childLevel++;
                }

            } else {
                itemPath = joinPath(separator, parentId, name);
                onItem(childLevel, typeName + ":" + itemPath, type, name);
                childLevel++;
            }

            if (children != null) {
                parseArray(childLevel, children, itemPath, type);
            }
        } else {
            LOG.debug("Incomplete item found: " + json);
        }

        json.endObject();
    }

    private String joinPath(@NotNull String delimiter, @NotNull String parent, @NotNull String child) {
        return parent.isEmpty() ? child : parent + delimiter + child;
    }

    @NotNull
    private static String findItemSeparator(@NotNull ClassMapItemType parentType, Boolean isStatic, ClassMapItemType type) {
        // special handling for functions
        if (type == ClassMapItemType.Function) {
            return isStatic == Boolean.TRUE ? "." : "#";
        }
        return parentType.getSeparator();
    }
}
