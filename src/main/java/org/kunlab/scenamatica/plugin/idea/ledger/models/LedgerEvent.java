package org.kunlab.scenamatica.plugin.idea.ledger.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LedgerEvent extends AbstractLedgerContent
{
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_JAVADOC = "javadoc";
    private static final String KEY_JAVADOC_LINK = "javadoc_link";
    private static final String KEY_SOURCE = "source";

    String id;
    String name;
    String javadoc;
    String javadocLink;
    Source source;
    String description;

    public LedgerEvent(LedgerReference reference, String id, String name, String javadoc, String javadocLink, Source source, String description)
    {
        super(reference);
        this.id = id;
        this.name = name;
        this.javadoc = javadoc;
        this.javadocLink = javadocLink;
        this.source = source;
        this.description = description;
    }

    public LedgerEvent()
    {
        this(null, null, null, null, null, null, null);
    }

    @AllArgsConstructor
    @Getter
    public enum Source
    {
        SPIGOT("spigot"),
        PAPER("paper"),
        BUKKIT("bukkit"),
        WATERFALL("waterfall"),
        VELOCITY("velocity"),
        PURPUR("purpur");

        private final String id;

        public static Source fromId(String id)
        {
            for (Source source : values())
                if (source.id.equalsIgnoreCase(id))
                    return source;
            return null;
        }
    }
}
