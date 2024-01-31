package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Value;

import java.util.Map;
import java.util.stream.Collectors;

@Value
public class SchemaMeta
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    String definitionsDir;
    String actionsDir;
    Map<String, String[]> definitions;
    Map<String, Map<String, String>> actions;

    @JsonIgnore
    Map<String, String> groupByAction;

    public SchemaMeta(String definitionsDir,
                      String actionsDir,
                      Map<String, String[]> definitions,
                      Map<String, Map<String, String>> actions)
    {
        this.definitionsDir = definitionsDir;
        this.actionsDir = actionsDir;
        this.definitions = definitions;
        this.actions = actions;

        this.groupByAction = groupByAction(actions);
    }

    public SchemaMeta()
    {
        this(null, null, null, null);
    }

    public boolean isActionExists(String action)
    {
        return this.groupByAction.containsKey(action);
    }

    public String getActionGroup(String action)
    {
        return this.groupByAction.get(action);
    }

    public boolean isDefinitionExists(String definition)
    {
        for (String[] definitionGroup : this.definitions.values())
            for (String definitionName : definitionGroup)
                if (definitionName.equals(definition))
                    return true;

        return false;
    }

    private static Map<String, String> groupByAction(Map<String, ? extends Map<String, String>> actions)
    {
        return actions.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream().map(s -> Map.entry(s, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static SchemaMeta fromJSON(VirtualFile metaFile)
    {
        try
        {
            return MAPPER.readValue(metaFile.getInputStream(), SchemaMeta.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to parse meta file: " + metaFile.getPath(), e);
        }
    }
}
