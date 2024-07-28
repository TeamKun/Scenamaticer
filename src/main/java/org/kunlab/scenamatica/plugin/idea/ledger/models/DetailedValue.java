package org.kunlab.scenamatica.plugin.idea.ledger.models;

import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

public interface DetailedValue
{
    String getName();

    LedgerReference getType();

    String getDescription();

    Double getMin();

    Double getMax();

    LedgerAdmonition[] getAdmonitions();

    boolean isArray();

    boolean isRequiredOn(@NotNull ScenarioType type);
}
