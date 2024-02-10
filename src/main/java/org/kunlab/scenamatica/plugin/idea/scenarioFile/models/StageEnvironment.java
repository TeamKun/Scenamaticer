package org.kunlab.scenamatica.plugin.idea.scenarioFile.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.PropertyKey;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

@AllArgsConstructor
@Getter
public enum StageEnvironment
{
    OVER_WORLD("models.stageEnvironment.overWorld"),
    NETHER("models.stageEnvironment.nether"),
    END("models.stageEnvironment.end");

    @PropertyKey(resourceBundle = "messages.ScenamaticerBundle")
    private final String displayName;

    @Override
    public String toString()
    {
        return ScenamaticerBundle.of(this.displayName);
    }
}
