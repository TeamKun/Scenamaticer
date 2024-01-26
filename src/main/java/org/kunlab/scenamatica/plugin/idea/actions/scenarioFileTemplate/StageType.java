package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StageType
{
    NORMAL("Normal"),
    FLAT("Flat"),
    AMPLIFIED("Amplified"),
    LARGE_BIOMES("Large Biomes"),
    CUSTOM("Custom");

    private final String name;

    @Override
    public String toString()
    {
        return this.name;
    }
}
