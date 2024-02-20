package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MissingArgumentsInspector extends AbstractScenamaticaActionElementInspection
{
    public static final String ID = "MissingArguments";

    public MissingArgumentsInspector()
    {
        super(ID, "Missing arguments", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected boolean checkAction(@NotNull ProblemsHolder holder, SchemaResolver.@NotNull ScenarioAction action, @NotNull YAMLKeyValue actionKV)
    {
        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return true;

        List<YAMLKeyValue> missingKeys = collectMissingKeys(action, actionDef, action.getArguments());
        if (!missingKeys.isEmpty())
            reportMissingArguments(holder, missingKeys, action, actionDef, actionKV);

        return true;
    }

    private void reportMissingArguments(@NotNull ProblemsHolder holder,
                                        @NotNull List<? extends YAMLKeyValue> missingKeys,
                                        @NotNull SchemaResolver.ScenarioAction action,
                                        @NotNull SchemaAction actionDef,
                                        @NotNull YAMLKeyValue actionKV)
    {
        PsiElement displayElement = getRequirementStateDisplayBlock(actionKV);

        for (YAMLKeyValue argument : missingKeys)
        {
            holder.registerProblem(
                    displayElement,
                    ScenamaticerBundle.of(
                            "editor.inspections.missingArgument.title",
                            argument.getKeyText(),
                            actionDef.name(),
                            action.getType().getDisplayName()
                    )
            );
        }
    }

    private static List<YAMLKeyValue> collectMissingKeys(SchemaResolver.ScenarioAction action, SchemaAction actionDef, YAMLMapping args)
    {
        List<YAMLKeyValue> missingKeys = new ArrayList<>();
        for (Map.Entry<String, SchemaAction.ActionIO> inputs : actionDef.arguments().entrySet())
        {
            String name = inputs.getKey();
            SchemaAction.ActionIO inputDef = inputs.getValue();
            YAMLKeyValue input = null;
            if (args != null)
                input = args.getKeyValueByKey(name);

            if (input == null && inputDef.isRequiredOn(action.getType()))
                missingKeys.add(new MissingKey(name));
        }

        return missingKeys;
    }

    @NotNull
    private static PsiElement getRequirementStateDisplayBlock(@NotNull YAMLKeyValue actionKV)
    {
        YAMLMapping actionParent = (YAMLMapping) actionKV.getParent();
        YAMLKeyValue argumentsBlockKV;
        if ((argumentsBlockKV = actionParent.getKeyValueByKey("with")) != null)
        {
            PsiElement key = argumentsBlockKV.getKey();
            return Objects.requireNonNullElse(key, argumentsBlockKV);
        }
        else
            return actionParent;
    }

    private static class MissingKey extends FakePsiElement implements YAMLKeyValue, SyntheticElement
    {
        private final String key;

        public MissingKey(String key)
        {
            this.key = key;
        }

        @Override
        public @Nullable PsiElement getKey()
        {
            return null;
        }

        @Override
        public @NotNull String getKeyText()
        {
            return this.key;
        }

        @Override
        public @Nullable YAMLValue getValue()
        {
            return null;
        }

        @Override
        public void setValue(@NotNull YAMLValue yamlValue)
        {

        }

        @Override
        public @NotNull String getValueText()
        {
            return "";
        }

        @Override
        public @Nullable YAMLMapping getParentMapping()
        {
            return null;
        }

        @Override
        public PsiElement getParent()
        {
            return null;
        }
    }
}
