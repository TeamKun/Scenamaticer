package org.kunlab.scenamatica.plugin.idea.ledger.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LedgerCategory extends AbstractLedgerContent
{
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PHASE = "phase";

    String id;
    String name;
    String phase;

    public LedgerCategory(LedgerReference reference, String id, String name, String phase)
    {
        super(reference);
        this.id = id;
        this.name = name;
        this.phase = phase;
    }

    public LedgerCategory()
    {
        this(null, null, null, null);
    }
}
