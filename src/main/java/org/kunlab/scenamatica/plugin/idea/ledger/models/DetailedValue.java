package org.kunlab.scenamatica.plugin.idea.ledger.models;

public interface DetailedValue
{
    String getName();

    LedgerReference getType();

    String getDescription();

    Double getMin();

    Double getMax();

    LedgerAdmonition[] getAdmonitions();

    boolean isArray();
}
