package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;

@Value
public class LedgerReference
{
    private static final String PREFIX = "$ref";
    private static final String DELIMITER = ":";

    String referenceType;
    String referenceBody;

    private LedgerReference(String referenceType, String referenceBody)
    {
        this.referenceType = referenceType;
        this.referenceBody = referenceBody;
    }

    @JsonCreator
    public static LedgerReference of(String reference)
    {
        // $ref:ledgerType:(category):1234567890 となる。すなはち, :つ以降は全てreferenceBodyとなる(:かどうかは関係なく)
        String[] parts = reference.split(DELIMITER, 3);
        if (parts.length < 3)
            throw new IllegalArgumentException("Broken reference format: " + reference);

        return new LedgerReference(parts[1], parts[2]);
    }
}
