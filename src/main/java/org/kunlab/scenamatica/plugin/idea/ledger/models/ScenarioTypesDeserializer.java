package org.kunlab.scenamatica.plugin.idea.ledger.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScenarioTypesDeserializer extends JsonDeserializer<ScenarioType[]>
{
    @Override
    public ScenarioType[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonNode node = p.getCodec().readTree(p);
        List<ScenarioType> enumList = new ArrayList<>();

        if (node.isArray())
            for (JsonNode elem : node)
                enumList.add(ScenarioType.valueOf(elem.asText()));
        else if (node.isTextual())
            enumList.add(ScenarioType.valueOf(node.asText()));

        return enumList.toArray(new ScenarioType[0]);
    }
}
