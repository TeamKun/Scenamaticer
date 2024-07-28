package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(using = LedgerType.Deserializer.class)
public class LedgerType extends AbstractLedgerContent implements IDetailedPropertiesHolder
{
    protected static final String KEY_TYPE = "type";
    protected static final String KEY_ID = "id";
    protected static final String KEY_DESCRIPTION = "description";
    protected static final String KEY_CATEGORY = "category";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_CLASS_NAME = "class";
    protected static final String KEY_MAPPING_OF = "mapping_of";
    protected static final String KEY_PROPERTIES = "properties";
    protected static final String KEY_ADMONITIONS = "admonitions";

    private final String id;
    private final String name;
    private final String description;
    private final LedgerReference category;
    private final String className;
    private final String mappingOf;
    private final Map<String, Property> properties;
    private final LedgerAdmonition[] admonitions;

    public LedgerType()
    {
        this(null, null, null, null, null, null, null, null, null);
    }

    public LedgerType(LedgerReference reference, String id, String name, String description, LedgerReference category, String className, String mappingOf, Map<String, Property> properties, LedgerAdmonition[] admonitions)
    {
        super(reference);
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.className = className;
        this.mappingOf = mappingOf;
        this.properties = properties;
        this.admonitions = admonitions;
    }

    @Override
    public Map<String, ? extends DetailedValue> getDetailedProperties()
    {
        return this.properties;
    }

    public static class Deserializer extends JsonDeserializer<LedgerType>
    {
        @Override
        public LedgerType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            JsonNode node = p.readValueAsTree();
            if (node == null)
                return null;

            String type = node.get(KEY_TYPE).asText();
            if ("string".equals(type))
                return ctxt.readTreeAsValue(node, LedgerStringType.class);

            return deserializeCompileType(node, ctxt);
        }

        public static LedgerType deserializeCompileType(JsonNode node, DeserializationContext ctxt)
        {
            return new LedgerType(
                    getAs(node, KEY_REFERENCE, n -> ctxt.readTreeAsValue(n, LedgerReference.class)),
                    getAs(node, KEY_ID, JsonNode::asText),
                    getAs(node, KEY_NAME, JsonNode::asText),
                    getAs(node, KEY_DESCRIPTION, JsonNode::asText),
                    getAs(node, KEY_CATEGORY, n -> ctxt.readTreeAsValue(n, LedgerReference.class)),
                    getAs(node, KEY_CLASS_NAME, JsonNode::asText),
                    getAs(node, KEY_MAPPING_OF, JsonNode::asText),
                    getAs(node, KEY_PROPERTIES, n -> {
                        Map<String, Property> map = new LinkedHashMap<>();
                        n.fields().forEachRemaining(e -> {
                            try
                            {
                                map.put(e.getKey(), ctxt.readTreeAsValue(e.getValue(), Property.class));
                            }
                            catch (IOException ex)
                            {
                                throw new RuntimeException(ex);
                            }
                        });
                        return map;
                    }),
                    getAs(node, KEY_ADMONITIONS, n -> ctxt.readTreeAsValue(n, LedgerAdmonition[].class))
            );
        }

        private static <T> T getAs(JsonNode node, String key, IOFunction<? super JsonNode, T> mapper)
        {
            JsonNode child = node.get(key);
            try
            {
                if (child == null)
                    return null;
                return mapper.apply(child);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to read property: " + key, e);
            }
        }

        @FunctionalInterface
        private interface IOFunction<T, R>
        {
            R apply(T t) throws IOException;
        }
    }

    @Value
    @AllArgsConstructor
    public static class Property implements DetailedValue
    {
        public static final String KEY_PATTERN = "pattern";
        public static final String KEY_MIN = "min";
        public static final String KEY_MAX = "max";
        private static final String KEY_NAME = "name";
        private static final String KEY_TYPE = "type";
        private static final String KEY_DESCRIPTION = "description";
        private static final String KEY_ARRAY = "array";
        private static final String KEY_REQUIRED = "required";
        private static final String KEY_DEFAULT_VALUE = "default";
        private static final String KEY_ADMONITIONS = "admonitions";

        String name;
        LedgerReference type;
        String description;
        boolean required;
        boolean array;
        String pattern;
        Double min;
        Double max;
        Object defaultValue;
        LedgerAdmonition[] admonitions;

        public Property()
        {
            this(null, null, null, false, false, null, null, null, null, null);
        }

        @Override
        public boolean isRequiredOn(@NotNull ScenarioType type)
        {
            return this.required;
        }
    }
}
