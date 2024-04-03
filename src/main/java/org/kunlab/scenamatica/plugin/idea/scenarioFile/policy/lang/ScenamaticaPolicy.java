package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;

public interface ScenamaticaPolicy
{
    @Nullable MinecraftVersion getMinecraftVersion();
}
