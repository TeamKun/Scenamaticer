package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

import java.util.ArrayList;
import java.util.List;

public class UnsupportedActionUsageInspector extends AbstractScenamaticaActionElementInspection
{
    public static final String ID = "UnsupportedActionUsage";

    public UnsupportedActionUsageInspector()
    {
        super(ID, "UnsupportedActionUsage", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected boolean checkAction(@NotNull ProblemsHolder holder, SchemaResolver.@NotNull ScenarioAction action, @NotNull YAMLKeyValue actionKV)
    {
        checkActionUsage(holder, action, actionKV);
        checkActionArguments(holder, action, actionKV);

        return true;
    }

    private void checkActionArguments(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action, @NotNull YAMLKeyValue actionKV)
    {
        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return;

        List<YAMLKeyValue> missingKeys = new ArrayList<>();
        List<YAMLKeyValue> unavailableKeys = new ArrayList<>();
        collectInvalidKeys(action, actionDef, action.getArguments(), missingKeys, unavailableKeys);

    }

    private static void checkActionUsage(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action,
                                         @NotNull PsiElement element)
    {
        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return;

        if (!actionDef.isAvailableFor(action.getType()))
        {
            holder.registerProblem(
                    element,
                    ScenamaticerBundle.of(
                            "editor.inspections.unsupportedActionUsage.action.description.title",
                            action.getName(),
                            action.getType().getDisplayName()
                    )
            );
        }
    }

    private static void collectInvalidKeys(SchemaResolver.ScenarioAction action, SchemaAction actionDef, YAMLMapping args,
                                           List<? super YAMLKeyValue> missingKeys, List<? super YAMLKeyValue> unavailableKeys)
    {
        if (args == null)
            return;

        for (YAMLKeyValue input : args.getKeyValues())
        {
            String name = input.getKeyText();
            SchemaAction.ActionIO inputDef = actionDef.arguments().get(name);
            if (inputDef == null)
                unavailableKeys.add(input);
        }
    }

}
