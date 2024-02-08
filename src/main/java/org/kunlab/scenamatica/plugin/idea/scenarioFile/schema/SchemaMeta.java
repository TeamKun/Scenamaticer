package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Value
public class SchemaMeta
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    String definitionsDir;
    String actionsDir;
    String prime;
    Map<String, String[]> definitions;
    Map<String, ? extends Map<String, ActionDescriptor>> actions;

    @JsonIgnore
    Map<String, String> groupByAction;
    @JsonIgnore
    Map<String, String> groupByDefinition;

    @JsonCreator
    public SchemaMeta(@JsonProperty("definitionsDir") String definitionsDir,
                      @JsonProperty("actionsDir") String actionsDir,
                      @JsonProperty("prime") String prime,
                      @JsonProperty("definitions") Map<String, String[]> definitions,
                      @JsonProperty("actions") Map<String, ? extends Map<String, ActionDescriptor>> actions)
    {
        this.definitionsDir = definitionsDir;
        this.actionsDir = actionsDir;
        this.prime = prime;
        this.definitions = definitions;
        this.actions = actions;

        this.groupByAction = groupByAction(actions);
        this.groupByDefinition = groupByDefinition(definitions);
    }

    public boolean isActionExists(String action)
    {
        return this.groupByAction.containsKey(action);
    }

    public String getActionGroupOf(String action)
    {
        return this.groupByAction.get(action);
    }

    public String getDefinitionGroupOf(String definition)
    {
        return this.groupByDefinition.get(definition);
    }

    public ActionDescriptor getAction(String action)
    {
        return this.getAction(this.getActionGroupOf(action), action);
    }

    public ActionDescriptor getAction(String actionGroup, String action)
    {
        if (actionGroup == null)
        {
            if (this.isActionExists(action))
                return getAction(action);
            else
                return null;
        }
        return this.actions.get(actionGroup).get(action);
    }

    public boolean isDefinitionExists(String definition)
    {
        for (String[] definitionGroup : this.definitions.values())
            for (String definitionName : definitionGroup)
                if (definitionName.equals(definition))
                    return true;

        return false;
    }

    private Map<String, String> groupByDefinition(Map<String, String[]> definitions)
    {
        return definitions.entrySet().stream()
                .flatMap(entry -> Map.of(entry.getKey(), entry.getValue()).entrySet().stream()
                        .flatMap(e -> Arrays.stream(e.getValue()).map(s -> Map.entry(s, e.getKey()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, String> groupByAction(Map<String, ? extends Map<String, ActionDescriptor>> actions)
    {
        return actions.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream()
                        .map(action -> Map.entry(action, entry.getKey())))
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

    @Getter
    public static class ActionDescriptor
    {
        private final String file;
        private final String description;

        public ActionDescriptor()
        {
            this.file = null;
            this.description = null;
        }
    }
}
