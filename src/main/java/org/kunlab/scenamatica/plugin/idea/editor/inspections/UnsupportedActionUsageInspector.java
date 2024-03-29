package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.models.ScenarioType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

import java.util.Map;
import java.util.Optional;

public class UnsupportedActionUsageInspector extends AbstractScenamaticaActionElementInspection
{
    public static final String KEY_ACTION_KIND_OF = "$actionKindOf";

    public static final String ID = "UnsupportedActionUsage";

    public UnsupportedActionUsageInspector()
    {
        super(ID, "UnsupportedActionUsage", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected boolean checkAction(@NotNull ProblemsHolder holder, SchemaResolver.@NotNull ScenarioAction action, @NotNull YAMLKeyValue actionKV)
    {
        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return true;

        checkActionUsage(holder, action, actionDef, actionKV);
        checkActionArguments(holder, actionDef, action);

        return true;
    }

    private void checkActionArguments(@NotNull ProblemsHolder holder, @NotNull SchemaAction actionDef, @NotNull SchemaResolver.ScenarioAction action)
    {
        if (action.getArguments() == null)
            return;
        for (Map.Entry<String, SchemaAction.ActionIO> argumentDef : actionDef.arguments().entrySet())
        {
            SchemaAction.ActionIO argument = argumentDef.getValue();
            if (!argument.type().equals("action"))
                continue;

            String actionKindOf = extractActionKindOf(argument);
            if (actionKindOf == null)
                continue;

            YAMLKeyValue argumentKV = action.getArguments().getKeyValueByKey(argumentDef.getKey());
            if (argumentKV == null)
                continue;

            if (!(argumentKV.getValue() instanceof YAMLMapping argumentMapping))
                continue;

            Optional<String> actionNameOptional = extractActionName(argumentMapping);
            if (actionNameOptional.isEmpty())
                continue;

            String actionNameText = actionNameOptional.get();
            checkActionAvailability(holder, action, actionKindOf, argumentMapping, actionNameText);
        }
    }

    private String extractActionKindOf(SchemaAction.ActionIO argument)
    {
        return argument.getAdditionalData(KEY_ACTION_KIND_OF, String.class)
                .orElse(null);
    }

    private Optional<String> extractActionName(YAMLMapping argumentMapping)
    {
        YAMLKeyValue actionName = argumentMapping.getKeyValueByKey("action");
        if (actionName == null)
            return Optional.empty();
        String actionNameText = actionName.getValueText();
        if (actionNameText.startsWith("\"") && actionNameText.endsWith("\""))
            actionNameText = actionNameText.substring(1, actionNameText.length() - 1);
        return Optional.of(actionNameText);
    }

    private void checkActionAvailability(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action, String actionKindOf, YAMLMapping argumentMapping, String actionNameText)
    {
        SchemaAction argumentActionDef = SchemaProviderService.getProvider().getAction(actionNameText);
        if (argumentActionDef == null)
            return;

        if (!argumentActionDef.isAvailableFor(ScenarioType.of(actionKindOf)))
            registerProblem(holder, argumentMapping, action);
    }

    private static void checkActionUsage(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action,
                                         @NotNull SchemaAction actionDef, @NotNull PsiElement element)
    {
        if (!actionDef.isAvailableFor(action.getType()))
            registerProblem(holder, element, action);
    }

    private static void registerProblem(@NotNull ProblemsHolder holder, @NotNull PsiElement element, @NotNull SchemaResolver.ScenarioAction action)
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
