package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UnsupportedActionUsageInspector extends AbstractScenamaticaInspection
{
    public static final String ID = "UnsupportedActionUsage";

    public UnsupportedActionUsageInspector()
    {
        super(ID, "UnsupportedActionUsage", HighlightDisplayLevel.ERROR);
    }

    @Override
    public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly)
    {
        return ProgressManager.getInstance().runProcess(
                () -> ApplicationManager.getApplication().runReadAction((ThrowableComputable<ProblemDescriptor[], ProcessCanceledException>) () -> {
                    if (file.getProject().isDisposed())
                        return ProblemDescriptor.EMPTY_ARRAY;
                    else
                    {
                        ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
                        visitYamlFile((YAMLFile) file, holder);
                        return holder.getResultsArray();
                    }
                }),
                new EmptyProgressIndicator()
        );
    }

    private void visitYamlFile(@NotNull YAMLFile file, @NotNull ProblemsHolder holder)
    {
        if (!ScenarioFiles.isScenarioFile(file))
            return;

        Iterator<PsiElement> it = YAMLUtils.getDepthFirstIterator(file);
        while (it.hasNext())
        {
            PsiElement el = it.next();
            if (el instanceof YAMLKeyValue)
                visitYamlKV((YAMLKeyValue) el, holder);
        }
    }

    private void visitYamlKV(@NotNull YAMLKeyValue kv, @NotNull ProblemsHolder holder)
    {

        String key = SchemaProviderService.getResolver().getTypeName(kv);
        if (!"action".equals(key))
            return;

        SchemaResolver.ScenarioAction action = SchemaProviderService.getResolver().getAction(kv);
        if (action == null)
            return;

        checkAction(holder, action, kv);
        checkActionArguments(holder, action, kv);
    }

    private static void checkAction(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action,
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

    private static void checkActionArguments(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action,
                                             @NotNull PsiElement element)
    {
        if (action.getArguments() == null)
            return;

        SchemaAction actionDef = SchemaProviderService.getProvider().getAction(action.getName());
        if (actionDef == null)
            return;

        List<YAMLKeyValue> missingKeys = new ArrayList<>();
        List<YAMLKeyValue> unavailableKeys = new ArrayList<>();
        collectInvalidKeys(action, actionDef, action.getArguments(), missingKeys, unavailableKeys);

        YAMLMapping args = action.getArguments();
        YAMLKeyValue actionKV = (YAMLKeyValue) args.getParent();
        PsiElement actionKey = actionKV.getKey();
        if (!missingKeys.isEmpty())
        {

            for (YAMLKeyValue argument : missingKeys)
            {
                assert actionKey != null;
                holder.registerProblem(
                        actionKey,
                        ScenamaticerBundle.of(
                                "editor.inspections.unsupportedActionUsage.argument.missing.title",
                                argument.getKeyText(),
                                actionDef.name(),
                                action.getType().getDisplayName()
                        )
                );
            }
        }
        else if (!unavailableKeys.isEmpty())
            for (YAMLKeyValue argument : unavailableKeys)
            {
                holder.registerProblem(
                        argument,
                        ScenamaticerBundle.of(
                                "editor.inspections.unsupportedActionUsage.argument.unavailable.title",
                                argument.getKeyText(),
                                actionDef.name(),
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

        for (Map.Entry<String, SchemaAction.ActionIO> inputs : actionDef.arguments().entrySet())
        {
            String name = inputs.getKey();
            SchemaAction.ActionIO inputDef = inputs.getValue();

            YAMLKeyValue input = args.getKeyValueByKey(name);
            if (input == null && inputDef.isRequiredOn(action.getType()))
                missingKeys.add(new MissingKey(name));
            else if (input != null && !inputDef.isAvailableFor(action.getType()))
                unavailableKeys.add(input);
        }
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