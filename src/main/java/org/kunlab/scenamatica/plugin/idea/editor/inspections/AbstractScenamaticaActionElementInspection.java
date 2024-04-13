package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

public abstract class AbstractScenamaticaActionElementInspection extends AbstractScenarioFileInspection
{
    public AbstractScenamaticaActionElementInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        super(id, displayName, defaultLevel);
    }

    @Override
    protected boolean visitYamlKV(@NotNull ScenarioFile file, @NotNull YAMLKeyValue kv, @NotNull ProblemsHolder holder)
    {
        String keyName = kv.getKeyText();
        if (!("action".equals(keyName) || "runif".equals(keyName)))
            return true;

        String typeName = SchemaProviderService.getResolver().getTypeName(kv);
        if (!"actionKinds".equals(typeName))
            return true;

        SchemaResolver.ScenarioAction action = SchemaProviderService.getResolver().getAction(kv);
        if (action == null)
            return this.onActionNotFound(holder, kv);

        SchemaAction actionDefinition = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDefinition == null)
            return this.onActionNotFound(holder, kv);

        return this.checkAction(holder, action, actionDefinition, kv);
    }

    protected boolean onActionNotFound(@NotNull ProblemsHolder holder, @NotNull YAMLKeyValue actionKV)
    {
        return true;
    }

    protected abstract boolean checkAction(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action, @NotNull SchemaAction actionDefinition, @NotNull YAMLKeyValue actionKV);
}
