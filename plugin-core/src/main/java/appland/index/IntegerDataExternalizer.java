package appland.index;

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class IntegerDataExternalizer implements DataExternalizer<Integer> {
    static final IntegerDataExternalizer INSTANCE = new IntegerDataExternalizer();

    @Override
    public void save(@NotNull DataOutput out, Integer value) throws IOException {
        out.writeInt(value);
    }

    @Override
    public Integer read(@NotNull DataInput in) throws IOException {
        return in.readInt();
    }
}
