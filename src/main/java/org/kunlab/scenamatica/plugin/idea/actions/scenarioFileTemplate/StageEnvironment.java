package org.kunlab.scenamatica.plugin.idea.actions.scenarioFileTemplate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StageEnvironment
{
    OVER_WORLD("Over World"),
    NETHER("Nether"),
    END("End");

    private final String name;

    @Override
    public String toString()
    {
        return this.name;
    }
}
