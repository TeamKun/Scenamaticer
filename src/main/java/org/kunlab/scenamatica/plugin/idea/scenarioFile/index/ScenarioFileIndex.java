package org.kunlab.scenamatica.plugin.idea.scenarioFile.index;

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@NotNull
public record ScenarioFileIndex(String name,
                                String description,
                                String path,
                                MinecraftVersion serverVersion,
                                MinecraftVersion sinceVersion,
                                MinecraftVersion untilVersion) implements DataExternalizer<ScenarioFileIndex>
{

    public static final ScenarioFileIndex EXTERNALIZER = new ScenarioFileIndex(null, null, null, null, null, null);

    @Override
    public void save(@NotNull DataOutput dataOutput, ScenarioFileIndex scenarioFileIndex) throws IOException
    {
        dataOutput.writeUTF(scenarioFileIndex.name);
        dataOutput.writeUTF(scenarioFileIndex.description);
        dataOutput.writeUTF(scenarioFileIndex.path);
        dataOutput.writeInt(scenarioFileIndex.serverVersion.ordinal());
        if (scenarioFileIndex.sinceVersion == null)
            dataOutput.writeInt(-1);
        else
            dataOutput.writeInt(scenarioFileIndex.sinceVersion.ordinal());
        if (scenarioFileIndex.untilVersion == null)
            dataOutput.writeInt(-1);
        else
            dataOutput.writeInt(scenarioFileIndex.untilVersion.ordinal());
    }

    @Override
    public ScenarioFileIndex read(@NotNull DataInput dataInput) throws IOException
    {
        String name = dataInput.readUTF();
        String description = dataInput.readUTF();
        String path = dataInput.readUTF();
        MinecraftVersion serverVersion = MinecraftVersion.values()[dataInput.readInt()];
        MinecraftVersion sinceVersion = null;
        MinecraftVersion untilVersion = null;

        int sinceVersionOrdinal = dataInput.readInt();
        if (sinceVersionOrdinal != -1)
            sinceVersion = MinecraftVersion.values()[sinceVersionOrdinal];
        int untilVersionOrdinal = dataInput.readInt();
        if (untilVersionOrdinal != -1)
            untilVersion = MinecraftVersion.values()[untilVersionOrdinal];

        return new ScenarioFileIndex(name, description, path, serverVersion, sinceVersion, untilVersion);
    }
}
