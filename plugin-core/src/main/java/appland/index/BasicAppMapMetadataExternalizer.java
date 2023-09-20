package appland.index;

import appland.problemsView.model.TestStatus;
import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static appland.index.IndexUtil.readOptionalString;
import static appland.index.IndexUtil.writeOptionalString;

class BasicAppMapMetadataExternalizer implements DataExternalizer<BasicAppMapMetadata> {
    static final @NotNull BasicAppMapMetadataExternalizer INSTANCE = new BasicAppMapMetadataExternalizer();

    private BasicAppMapMetadataExternalizer() {
    }

    @Override
    public BasicAppMapMetadata read(@NotNull DataInput in) throws IOException {
        var name = readOptionalString(in);

        var testStatusValue = readOptionalString(in);
        var testStatus = testStatusValue != null ? TestStatus.byJsonId(testStatusValue) : null;

        var recorderType = readOptionalString(in);
        var recorderName = readOptionalString(in);
        var languageName = readOptionalString(in);

        return new BasicAppMapMetadata(name, testStatus, recorderType, recorderName, languageName);
    }

    @Override
    public void save(@NotNull DataOutput out, @NotNull BasicAppMapMetadata value) throws IOException {
        writeOptionalString(out, value.name);
        writeOptionalString(out, value.testStatus != null ? value.testStatus.getJsonId() : null);
        writeOptionalString(out, value.recorderType);
        writeOptionalString(out, value.recorderName);
        writeOptionalString(out, value.languageName);
    }
}
