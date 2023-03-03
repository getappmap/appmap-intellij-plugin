package appland.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonReaderEx;

/**
 * Iterate a classMap.json file with GSON's streaming parser to reduce memory usage of the indexer.
 */
abstract class StreamingClassMapIterator {
    private static final Logger LOG = Logger.getInstance(StreamingClassMapIterator.class);

    protected abstract void onItem(@NotNull ClassMapItemType type,
                                   @Nullable String parentId,
                                   @NotNull String id,
                                   @NotNull String name,
                                   @Nullable String location,
                                   int level);

    public void parse(@NotNull CharSequence content) {
        if (content.length() == 0) {
            return;
        }

        try (var json = new JsonReaderEx(content)) {
            parseArray(json, 0, "", ClassMapItemType.ROOT);
        }
    }

    private void parseArray(@NotNull JsonReaderEx json, int level, @Nullable String parentId, @NotNull ClassMapItemType parentType) {
        json.beginArray();

        while (json.hasNext()) {
            var childrenReader = json.createSubReaderAndSkipValue();
            assert childrenReader != null;

            parseItem(childrenReader, level, parentId, parentType);
        }

        json.endArray();
    }

    @NotNull
    private static String findItemSeparator(@NotNull ClassMapItemType parentType, @NotNull ClassMapItemType type, @Nullable Boolean isStatic) {
        // special handling for functions
        if (type == ClassMapItemType.Function) {
            return isStatic == Boolean.TRUE ? "." : "#";
        }
        return parentType.getSeparator();
    }

    private String joinPath(@NotNull String delimiter, @Nullable String parent, @NotNull String child) {
        return parent == null || parent.isEmpty() ? child : parent + delimiter + child;
    }

    private void parseItem(@NotNull JsonReaderEx json, int level, @Nullable String parentId, @NotNull ClassMapItemType parentType) {
        json.beginObject();

        String name = null;
        String typeName = null;
        String location = null;
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
                case "location":
                    location = json.nextString();
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
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Skipping classMap property: " + property);
                    }
                    break;
            }
        }

        if (name != null && typeName != null) {
            var type = ClassMapItemType.findByName(typeName);
            var separator = findItemSeparator(parentType, type, isStatic);
            var parentIdWithType = StringUtil.isEmpty(parentId)
                    ? parentType.getName()
                    : parentType.getName() + ":" + parentId;

            // Some code object entries have a path-delimited package name,
            // but we want each package name token to be its own object.
            String itemPath;
            var childLevel = level;
            if (type == ClassMapItemType.Package) {
                itemPath = parentId;
                for (var parentName : StringUtil.split(name, type.getSeparator())) {
                    itemPath = joinPath(type.getSeparator(), itemPath, parentName);
                    onItem(type, parentIdWithType, typeName + ":" + itemPath, name, location, childLevel);
                    childLevel++;
                }
            } else {
                itemPath = joinPath(separator, parentId, name);
                onItem(type, parentIdWithType, typeName + ":" + itemPath, name, location, childLevel);
                childLevel++;
            }

            if (children != null) {
                parseArray(children, childLevel, itemPath, type);
            }
        } else {
            LOG.debug("Incomplete item found: " + json);
        }

        json.endObject();
    }
}
