package appland.index;

import appland.problemsView.model.TestStatus;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class BasicAppMapMetadataExternalizer implements DataExternalizer<BasicAppMapMetadata> {
    public static final BasicAppMapMetadataExternalizer INSTANCE = new BasicAppMapMetadataExternalizer();

    @Override
    public BasicAppMapMetadata read(@NotNull DataInput in) throws IOException {
        var name = IOUtil.readUTF(in);
        var hasTestStatus = in.readBoolean();

        TestStatus testStatus = null;
        if (hasTestStatus) {
            var jsonId = IOUtil.readUTF(in);
            testStatus = TestStatus.byJsonId(jsonId);
        }

        return new BasicAppMapMetadata(name, testStatus);
    }

    @Override
    public void save(@NotNull DataOutput out, BasicAppMapMetadata value) throws IOException {
        IOUtil.writeUTF(out, value.name);

        out.writeBoolean(value.testStatus != null);
        if (value.testStatus != null) {
            IOUtil.writeUTF(out, value.testStatus.getJsonId());
        }
    }
}
