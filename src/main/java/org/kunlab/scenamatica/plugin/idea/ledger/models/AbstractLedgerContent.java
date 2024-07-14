package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractLedgerContent implements ILedgerContent
{
    protected static final String KEY_REFERENCE = "$reference";

    @JsonProperty("$reference")
    private final LedgerReference reference;

    public AbstractLedgerContent()
    {
        this(null);
    }

    public AbstractLedgerContent(LedgerReference reference)
    {
        this.reference = reference;
    }

    @Override
    public LedgerReference getReference()
    {
        return this.reference;
    }
}
