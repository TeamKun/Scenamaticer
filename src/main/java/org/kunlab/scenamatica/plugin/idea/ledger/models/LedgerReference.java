package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;

import java.util.Arrays;

@Value
public class LedgerReference
{
    private static final String PREFIX = "$ref";
    private static final String DELIMITER = ":";

    Type referenceType;
    String referenceBody;

    private LedgerReference(Type referenceType, String referenceBody)
    {
        this.referenceType = referenceType;
        this.referenceBody = referenceBody;
    }

    public boolean canResolve()
    {
        // 1. referenceType が null でないこと
        // 2. referenceBody が null でないこと
        // 3. referenceBody が空文字でないこと
        // 4. referenceBody が ? で始まっていないこと
        // => $ref:type:?category のような形式は解決できない
        return !(this.referenceType == null || this.referenceBody == null ||
                this.referenceBody.isEmpty() || this.referenceBody.startsWith("?"));
    }

    @JsonCreator
    public static LedgerReference of(String reference)
    {
        // $ref:ledgerType:(category):1234567890 となる。すなはち, :つ以降は全てreferenceBodyとなる(:かどうかは関係なく)
        // また, プリミティブ型はその名前のみなので, type リファレンスに飛ばす。
        String[] parts = reference.split(DELIMITER, 3);
        if (parts.length == 1 && LedgerScenarioResolver.PrimitiveType.isPrimitiveType(parts[0]))
            return new LedgerReference(Type.TYPES, parts[0]);
        if (parts.length < 3)
            throw new IllegalArgumentException("Broken reference format: " + reference);

        String type = parts[1];
        String body = parts[2];

        if (!Type.isValidType(type))
            throw new IllegalArgumentException("Unknown reference type: " + type);

        return new LedgerReference(Type.byName(type), body);
    }

    @Getter
    @AllArgsConstructor
    public enum Type
    {
        ACTION("action", LedgerAction.class),
        TYPES("type", LedgerType.class),
        CATEGORIES("category", LedgerCategory.class),
        EVENTS("event", LedgerEvent.class);

        @NotNull
        private final String name;
        @NotNull
        private final Class<? extends ILedgerContent> clazz;

        public static boolean isValidType(@NotNull String type)
        {
            return Arrays.stream(Type.values()).anyMatch(t -> t.name.equals(type));
        }

        public static Type byName(@NotNull String name)
        {
            return Arrays.stream(Type.values())
                    .filter(t -> t.name.equals(name)).findFirst()
                    .orElse(null);
        }
    }
}
