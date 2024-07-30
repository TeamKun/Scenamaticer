package org.kunlab.scenamatica.plugin.idea.ledger.models;

import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

import java.util.Map;

public interface DetailedValue
{
    String getName();

    LedgerReference getType();

    String getDescription();

    Double getMin();

    Double getMax();

    StringFormat getFormat();

    String getPattern();

    Map<String, String> getEnums();

    LedgerAdmonition[] getAdmonitions();

    boolean isArray();

    boolean isRequiredOn(@NotNull ScenarioType type);
}
