package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.project.Project;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SchemaProvider implements JsonSchemaProviderFactory
{
    @Override
    public @NotNull List<JsonSchemaFileProvider> getProviders(@NotNull Project project)
    {
        return List.of(new ScenarioSchemaProvider(project));
    }
}
