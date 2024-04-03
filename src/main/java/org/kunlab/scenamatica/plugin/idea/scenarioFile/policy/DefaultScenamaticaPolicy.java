package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy;

import lombok.Getter;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;

@Getter
public class DefaultScenamaticaPolicy implements ScenamaticaPolicy
{
    public static final DefaultScenamaticaPolicy INSTANCE = new DefaultScenamaticaPolicy();
    private final MinecraftVersion minecraftVersion = MinecraftVersion.ANY;

    private DefaultScenamaticaPolicy()
    {
    }
}
