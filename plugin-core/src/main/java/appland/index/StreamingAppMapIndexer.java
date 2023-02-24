package appland.index;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.SingleEntryIndexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonReaderEx;

/**
 * Uses GSON's streaming JSON reader to minimize the memory usage.
 */
class StreamingAppMapIndexer extends SingleEntryIndexer<AppMapMetadata> {
    private static final Logger LOG = Logger.getInstance("#appmap.index");

    public StreamingAppMapIndexer() {
        super(false);
    }

    @Override
    protected @Nullable AppMapMetadata computeValue(@NotNull FileContent inputData) {
        String name = null;
        String sourceLocation = null;
        var request_query_functions = new int[3];

        var content = inputData.getContentAsText();
        if (content.length() == 0) {
            return null;
        }

        try (var json = new JsonReaderEx(content)) {
            json.beginObject(); // top-level {...}
            while (true) {
                var propertyName = json.nextNameOrNull();
                if (propertyName == null) {
                    break;
                }

                switch (propertyName) {
                    case "metadata": {
                        // metadata: {...}
                        var subReader = json.createSubReaderAndSkipValue();
                        if (subReader != null) {
                            try (subReader) {
                                var nameAndSourceLocation = readMetadataNameAndSourceLocation(subReader);
                                name = nameAndSourceLocation != null ? nameAndSourceLocation.first : null;
                                sourceLocation = nameAndSourceLocation != null ? nameAndSourceLocation.second : null;
                            }
                        }
                        break;
                    }
                    case "events": {
                        var subReader = json.createSubReaderAndSkipValue();
                        if (subReader != null) {
                            try (subReader) {
                                readEvents(subReader, request_query_functions);
                            }
                        }
                        break;
                    }
                    case "classMap": {
                        var subReader = json.createSubReaderAndSkipValue();
                        if (subReader != null) {
                            try (subReader) {
                                readClassMap(subReader, request_query_functions);
                            }
                        }
                        break;
                    }
                    default:
                        json.skipValue();
                        break;
                }
            }
        } catch (Exception e) {
            LOG.debug("parsing AppMap JSON file failed: " + inputData.getFile().getPath(), e);
            return null;
        }

        if (name == null) {
            return null;
        }

        var requestCount = request_query_functions[0];
        var queryCount = request_query_functions[1];
        var functionsCount = request_query_functions[2];
        return new AppMapMetadata(name, sourceLocation, inputData.getFile().getPath(), requestCount, queryCount, functionsCount);
    }

    private @Nullable Pair<@NotNull String, @Nullable String> readMetadataNameAndSourceLocation(@NotNull JsonReaderEx json) {
        String name = null;
        String sourceLocation = null;

        json.beginObject();
        while (true) {
            var propertyName = json.nextNameOrNull();
            if (propertyName == null) {
                break;
            } else if ("name".equals(propertyName)) {
                name = json.nextNullableString();
            } else if ("source_location".equals(propertyName)) {
                sourceLocation = json.nextNullableString();
            } else {
                json.skipValue();
            }
        }
        json.endObject();
        return name == null ? null : Pair.create(name, sourceLocation);
    }

    private void readEvents(@NotNull JsonReaderEx json, int[] request_query_functions) {
        json.beginArray();

        while (json.hasNext()) {
            var object = json.beginObject();

            while (true) {
                var propertyName = object.nextNameOrNull();
                if (propertyName == null) {
                    break;
                } else if ("http_server_request".equals(propertyName)) {
                    request_query_functions[0]++;
                } else if ("sql_query".equals(propertyName)) {
                    request_query_functions[1]++;
                }

                json.skipValue();
            }

            json.endObject();
        }

        json.endArray();
    }

    /**
     * Reads a "classMap" property and increments the functions count in request_query_functions.
     * It recursively iterates the classMap and all subelements.
     * <p>
     * Precondition: json must be at the start of an array
     */
    private void readClassMap(JsonReaderEx json, int[] request_query_functions) {
        json.beginArray();

        while (json.hasNext()) {
            json.beginObject();

            while (true) {
                var propertyName = json.nextNameOrNull();
                if (propertyName == null) {
                    break;
                }

                if ("type".equals(propertyName)) {
                    var propertyValue = json.nextString();
                    if ("function".equals(propertyValue)) {
                        request_query_functions[2]++;
                    }
                    continue;
                }

                if ("children".equals(propertyName)) {
                    var subReader = json.createSubReaderAndSkipValue();
                    if (subReader != null) {
                        readClassMap(subReader, request_query_functions);
                    }
                } else {
                    json.skipValue();
                }
            }

            json.endObject();
        }

        json.endArray();
    }
}
