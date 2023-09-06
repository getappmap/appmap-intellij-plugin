package appland.index;

import appland.problemsView.model.TestStatus;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class BasicAppMapMetadataExternalizer implements DataExternalizer<BasicAppMapMetadata> {
    static final @NotNull BasicAppMapMetadataExternalizer INSTANCE = new BasicAppMapMetadataExternalizer();

    private BasicAppMapMetadataExternalizer() {
    }

    @Override
    public BasicAppMapMetadata read(@NotNull DataInput in) throws IOException {
        var hasName = in.readBoolean();
        var name = hasName ? IOUtil.readUTF(in) : null;

        var hasTestStatus = in.readBoolean();
        TestStatus testStatus = null;
        if (hasTestStatus) {
            var jsonId = IOUtil.readUTF(in);
            testStatus = TestStatus.byJsonId(jsonId);
        }

        return new BasicAppMapMetadata(name, testStatus);
    }

    @Override
    public void save(@NotNull DataOutput out, @NotNull BasicAppMapMetadata value) throws IOException {
        out.writeBoolean(value.name != null);
        if (value.name != null) {
            IOUtil.writeUTF(out, value.name);
        }

        out.writeBoolean(value.testStatus != null);
        if (value.testStatus != null) {
            IOUtil.writeUTF(out, value.testStatus.getJsonId());
        }
    }
}
