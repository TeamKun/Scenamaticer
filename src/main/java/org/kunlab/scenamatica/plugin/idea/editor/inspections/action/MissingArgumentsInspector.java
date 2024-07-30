package org.kunlab.scenamatica.plugin.idea.editor.inspections.action;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.jetbrains.jsonSchema.impl.JsonSchemaType;
import com.jetbrains.jsonSchema.impl.JsonValidationError;
import com.jetbrains.jsonSchema.impl.fixes.AddMissingPropertyFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.schema.YamlJsonPsiWalker;
import org.kunlab.scenamatica.plugin.idea.editor.inspections.AbstractScenarioFileInspection;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

import java.util.Objects;
import java.util.stream.Collectors;

public class MissingArgumentsInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "MissingArguments";

    public MissingArgumentsInspector()
    {
        super(ID, "Missing arguments", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        this.reportDetailedResolveErrorTypeOf(file, holder, session,
                (missingArguments) -> new AddMissingPropertyFix(
                        new JsonValidationError.MissingMultiplePropsIssueData(
                                missingArguments.stream()
                                        .map((resolve) -> {
                                            PsiElement element = resolve.getElement();
                                            PsiElement yamlKey;
                                            if (!(element instanceof YAMLKeyValue keyValue))
                                                return null;
                                            else if ((yamlKey = keyValue.getKey()) == null)
                                                return null;

                                            return new JsonValidationError.MissingPropertyIssueData(
                                                    yamlKey.getText(),
                                                    JsonSchemaType._any,
                                                    null,
                                                    0
                                            );
                                        })
                                        .filter(Objects::isNull)
                                        .collect(Collectors.toList())
                        ),
                        YamlJsonPsiWalker.INSTANCE.getSyntaxAdapter(holder.getProject())
                ),
                this::traverseKVKey,
                LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_INPUT_MISSING_REQUIRED
        );
    }
}
