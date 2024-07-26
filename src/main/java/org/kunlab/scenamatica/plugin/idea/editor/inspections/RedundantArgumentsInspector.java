package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.editor.fixes.DeleteElementFix;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

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
        this.reportDetailedResolveErrorTypeOf(file, holder, session,
                (unsupportedActions) -> new DeleteElementFix(
                        unsupportedActions.stream()
                                .map(LedgerScenarioResolver.ResolveResult::getElement)
                                .toArray(PsiElement[]::new)
                ),
                LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_INPUT_REDUNDANT
        );
    }
}
