package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class EnumsDeserializer extends JsonDeserializer<Map<String, String>>
{
    @Override
    public Map<String, String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
    {
        return deserialize(jsonParser.getCodec().readTree(jsonParser));
    }

    public static Map<String, String> deserialize(JsonNode node) throws IOException
    {
        Map<String, String> resultMap = new HashMap<>();

        if (node.isArray())
        {
            // JSONが配列である場合
            for (JsonNode element : node)
                if (element.isTextual())
                    resultMap.put(element.textValue().toUpperCase(Locale.ENGLISH), null);
        }
        else if (node.isObject())
        {
            // JSONがオブジェクトである場合
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext())
            {
                Map.Entry<String, JsonNode> field = fields.next();
                resultMap.put(field.getKey().toUpperCase(Locale.ENGLISH), field.getValue().textValue());
            }
        }

        return resultMap;
    }
}
