package org.kunlab.scenamatica.plugin.idea.scenarioFile.models;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.PropertyKey;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

@AllArgsConstructor
public enum StageType
{
    NORMAL("models.stageType.normal"),
    FLAT("models.stageType.flat"),
    AMPLIFIED("models.stageType.amplified"),
    LARGE_BIOMES("models.stageType.largeBiomes"),
    CUSTOM("models.stageType.custom");

    @PropertyKey(resourceBundle = "messages.ScenamaticerBundle")
    private final String displayName;

    @Override
    public String toString()
    {
        return ScenamaticerBundle.of(this.displayName);
    }
}
