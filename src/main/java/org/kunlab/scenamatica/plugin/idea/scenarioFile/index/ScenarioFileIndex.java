package org.kunlab.scenamatica.plugin.idea.scenarioFile.index;

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record ScenarioFileIndex(String name,
                                String description,
                                String path) implements DataExternalizer<ScenarioFileIndex>
{

    public static final ScenarioFileIndex EXTERNALIZER = new ScenarioFileIndex(null, null, null);

    @Override
    public void save(@NotNull DataOutput dataOutput, ScenarioFileIndex scenarioFileIndex) throws IOException
    {
        dataOutput.writeUTF(scenarioFileIndex.name);
        dataOutput.writeUTF(scenarioFileIndex.description);
        dataOutput.writeUTF(scenarioFileIndex.path);
    }

    @Override
    public ScenarioFileIndex read(@NotNull DataInput dataInput) throws IOException
    {
        return new ScenarioFileIndex(
                dataInput.readUTF(),
                dataInput.readUTF(),
                dataInput.readUTF()
        );
    }
}
