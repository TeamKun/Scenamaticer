package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.editor.fixes.DeleteElementFix;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedundantArgumentsInspector extends AbstractScenamaticaActionElementInspection
{
    public static final String ID = "RedundantArguments";
    private static WeakReference<RedundantArgumentsInspector> instance;

    public RedundantArgumentsInspector()
    {
        super(ID, "Redundant arguments", HighlightDisplayLevel.WARNING);

        instance = new WeakReference<>(this);
    }

    @Override
    protected boolean checkAction(@NotNull ProblemsHolder holder, SchemaResolver.@NotNull ScenarioAction action, @NotNull YAMLKeyValue actionKV)
    {
        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return true;

        List<YAMLKeyValue> redundantKeys = collectRedundantKeys(action, actionDef, action.getArguments());
        if (!redundantKeys.isEmpty())
            reportRedundantArguments(holder, redundantKeys, action, actionDef);

        return true;
    }

    private void reportRedundantArguments(@NotNull ProblemsHolder holder,
                                          @NotNull List<? extends YAMLKeyValue> unavailableKeys,
                                          @NotNull SchemaResolver.ScenarioAction action,
                                          @NotNull SchemaAction actionDef)
    {
        DeleteElementFix fix = new DeleteElementFix(unavailableKeys.toArray(new YAMLKeyValue[0]));
        for (YAMLKeyValue argument : unavailableKeys)
        {
            holder.registerProblem(
                    argument,
                    ScenamaticerBundle.of(
                            "editor.inspections.redundantArgumentUsage.title",
                            argument.getKeyText(),
                            actionDef.name(),
                            action.getType().getDisplayName()
                    ),
                    fix
            );
        }
    }

    private static List<YAMLKeyValue> collectRedundantKeys(SchemaResolver.ScenarioAction action, SchemaAction actionDef, YAMLMapping args)
    {
        if (args == null)
            return Collections.emptyList();

        List<YAMLKeyValue> redundantKeys = new ArrayList<>();
        for (YAMLKeyValue input : args.getKeyValues())
        {
            String name = input.getKeyText();
            SchemaAction.ActionIO inputDef = actionDef.arguments().get(name);
            if (inputDef == null)
                redundantKeys.add(input);
        }

        return redundantKeys;
    }

    public static RedundantArgumentsInspector getInstance()
    {
        return instance == null ? null: instance.get();
    }

}
