package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.jetbrains.jsonSchema.impl.JsonSchemaType;
import com.jetbrains.jsonSchema.impl.JsonValidationError;
import com.jetbrains.jsonSchema.impl.fixes.AddMissingPropertyFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.schema.YamlJsonPsiWalker;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MissingArgumentsInspector extends AbstractScenamaticaInspection
{
    public static final String ID = "MissingArguments";

    public MissingArgumentsInspector()
    {
        super(ID, "Missing arguments", HighlightDisplayLevel.ERROR);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session)
    {
        return new PsiElementVisitor()
        {
            @Override
            public void visitFile(@NotNull PsiFile file)
            {
                if (!(file instanceof ScenarioFile scenarioFile))
                    return;

                LedgerScenarioResolver resolveResults = LedgerScenarioResolver.create(
                        LedgerManagerService.getInstance(),
                        scenarioFile,
                        session
                ).detailedResolve();

                List<LedgerScenarioResolver.ResolveResult> missingArguments =
                        resolveResults.getErrors(LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_INPUT_MISSING_REQUIRED);

                AddMissingPropertyFix fix = new AddMissingPropertyFix(
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
                );

                for (LedgerScenarioResolver.ResolveResult missingArgument : missingArguments)
                {
                    holder.registerProblem(
                            missingArgument.getElement(),
                            keyTextRangeOf((YAMLKeyValue) missingArgument.getElement()),
                            missingArgument.getInvalidMessage(),
                            fix
                    );
                }
            }
        };
    }
}
