package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public LedgerStringType()
    {
        super();
        this.format = null;
        this.pattern = null;
        this.enums = null;
    }

    public LedgerStringType(LedgerType original, Format format, String pattern, Map<String, String> enums)
    {
        super(original.getReference(), original.getId(), original.getName(), original.getDescription(), original.getCategory(), original.getClassName(), original.getMappingOf(), original.getProperties(), original.getAdmonitions());
        this.format = format;
        this.enums = enums;
        this.pattern = pattern;
    }

    public Pattern getPattern()
    {
        return this.pattern == null ? null: Pattern.compile(this.pattern);
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
    public enum Format
    {
        DATE_TIME("date-time", "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2}|Z)?)"),
        DATE("date", "\\d{4}-\\d{2}-\\d{2}"),
        TIME("time", "\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([+-]\\d{2}:\\d{2}|Z)?"),
        DURATION("duration", "P(?:\\d+Y)?(?:\\d+M)?(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?"),

        EMAIL("email", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
        IDN_EMAIL("idn-email", "[\\p{L}0-9._%+-]+@[\\p{L}0-9.-]+\\.[a-zA-Z]{2,}"),

        HOSTNAME("hostname", "[a-zA-Z0-9.-]{1,253}"),
        IDN_HOSTNAME("idn-hostname", "[\\p{L}0-9.-]{1,253}"),

        IPV4("ipv4", "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"),
        IPV6("ipv6", "(?:[\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}"),

        URI("uri", "(https?|ftp|file)://[\\S]+"),
        URI_REFERENCE("uri-reference", "[\\S]+"),
        IRI("iri", "(https?|ftp|file)://[\\S\\p{L}]+"),
        IRI_REFERENCE("iri-reference", "[\\S\\p{L}]+"),

        UUID("uuid", "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

        private final String value;
        private final Pattern pattern;

        Format(String value, String patternString)
        {
            this.value = value;
            this.pattern = Pattern.compile("^" + patternString + "$");
        }

        @JsonDeserialize
        public static Format fromString(String value)
        {
            if (value == null)
                return null;

            for (Format format : Format.values())
            {
                if (format.value.equals(value))
                    return format;
            }

            return null;
        }
    }

    public static class Deserializer extends JsonDeserializer<LedgerStringType>
    {
        @Override
        public LedgerStringType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
        {
            JsonNode node = jsonParser.readValueAs(JsonNode.class);
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

            return new LedgerStringType(action, Format.fromString(format), pattern, enums);
        }
    }
}
