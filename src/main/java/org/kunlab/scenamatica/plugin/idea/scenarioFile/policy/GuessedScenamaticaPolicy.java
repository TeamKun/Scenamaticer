package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy;

import lombok.Value;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;

@Value
public class GuessedScenamaticaPolicy implements ScenamaticaPolicy
{
    MinecraftVersion minecraftVersion;
}
