package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(using = LedgerStringType.Deserializer.class)
public class LedgerStringType extends LedgerType implements IPrimitiveType
{
    public static final int FORMAT_TYPE = 0;
    public static final int PATTERN_TYPE = 1;
    public static final int ENUMS_TYPE = 2;
    public static final int DEFAULT_TYPE = 3;

    private static final String KEY_FORMAT = "format";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_ENUMS = "enums";

    private final Format format;
    private final String pattern;
    private final Map<String, String> enums;

    public LedgerStringType(LedgerType original, Format format, String pattern, Map<String, String> enums)
    {
        super(original.getReference(), original.getId(), original.getName(), original.getDescription(), original.getCategory(), original.getClassName(), original.getMappingOf(), original.getProperties(), original.getAdmonitions());
        this.format = format;
        this.enums = enums;
        this.pattern = pattern;
    }

    public int type()
    {
        if (this.format != null)
            return FORMAT_TYPE;
        else if (this.pattern != null)
            return PATTERN_TYPE;
        else if (this.enums != null)
            return ENUMS_TYPE;
        else
            return DEFAULT_TYPE;
    }

    private static Map<String, String> toMap(List<String> enums)
    {
        Map<String, String> map = new HashMap<>();
        for (String e : enums)
            map.put(e, null);

        return map;
    }

    @Getter
    @AllArgsConstructor
    public enum Format
    {
        DATE_TIME("date-time"),
        DATE("date"),
        TIME("time"),
        DURATION("duration"),

        EMAIL("email"),
        IDN_EMAIL("idn-email"),

        HOSTNAME("hostname"),
        IDN_HOSTNAME("idn-hostname"),

        IPV4("ipv4"),
        IPV6("ipv6"),

        URI("uri"),
        URI_REFERENCE("uri-reference"),
        IRI("iri"),
        IRI_REFERENCE("iri-reference"),
        UUID("uuid");

        private final String value;

    }

    public static class Deserializer extends JsonDeserializer<LedgerStringType>
    {
        @Override
        public LedgerStringType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.readValueAsTree();
            if (node == null)
                return null;

            // そうでなければ, CompiledType を 通常通りデシリアライズして, TypeReference のコンストラクタに渡す.
            LedgerType action = LedgerType.Deserializer.deserializeCompileType(node, deserializationContext);

            String format = node.has(KEY_FORMAT) ? node.get(KEY_FORMAT).asText(): null;
            String pattern = node.has(KEY_PATTERN) ? node.get(KEY_PATTERN).asText(): null;
            Map<String, String> enums = null;
            if (node.has(KEY_ENUMS))
            {
                JsonNode enumsNode = node.get(KEY_ENUMS);
                if (enumsNode.isArray())
                {
                    enums = new HashMap<>();
                    for (JsonNode e : enumsNode)
                        enums.put(e.asText(), null);
                }
                else
                {
                    enums = new HashMap<>();
                    for (JsonNode e : enumsNode)
                        enums.put(e.asText(), enumsNode.get(e.asText()).asText());
                }
            }

            return new LedgerStringType(action, Format.valueOf(format), pattern, enums);
        }
    }
}
