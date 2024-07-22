package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.editor.fixes.DeleteElementFix;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

import java.util.List;

public class RedundantArgumentsInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "RedundantArguments";

    public RedundantArgumentsInspector()
    {
        super(ID, "Redundant arguments", HighlightDisplayLevel.WARNING);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        LedgerScenarioResolver resolveResults = LedgerScenarioResolver.create(
                LedgerManagerService.getInstance(),
                file,
                session
        ).detailedResolve();

        List<LedgerScenarioResolver.ResolveResult> redundantArguments =
                resolveResults.getErrors(LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_INPUT_REDUNDANT);

        DeleteElementFix fix = new DeleteElementFix(
                redundantArguments.stream()
                        .map(LedgerScenarioResolver.ResolveResult::getElement)
                        .toArray(PsiElement[]::new)
        );

        for (LedgerScenarioResolver.ResolveResult resolve : redundantArguments)
        {
            YAMLKeyValue element = (YAMLKeyValue) resolve.getElement();

            holder.registerProblem(
                    element,
                    keyTextRangeOf((YAMLKeyValue) resolve.getElement()),
                    resolve.getInvalidMessage(),
                    fix
            );
        }
    }
}
