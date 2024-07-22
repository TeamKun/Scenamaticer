package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

import java.util.List;

public class UnsupportedActionUsageInspector extends AbstractScenarioFileInspection
{
    public static final String ID = "UnsupportedActionUsage";

    public UnsupportedActionUsageInspector()
    {
        super(ID, "UnsupportedActionUsage", HighlightDisplayLevel.ERROR);
    }

    @Override
    protected void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session)
    {
        LedgerScenarioResolver resolveResults = LedgerScenarioResolver.create(
                LedgerManagerService.getInstance(),
                file,
                session
        ).detailedResolve();

        List<LedgerScenarioResolver.ResolveResult> unsupportedActions =
                resolveResults.getErrors(LedgerScenarioResolver.ResolveResult.InvalidCause.ACTION_USAGE_VIOLATION);

        for (LedgerScenarioResolver.ResolveResult unsupportedAction : unsupportedActions)
        {
            PsiElement targetElement = unsupportedAction.getElement();
            if (targetElement instanceof YAMLKeyValue kv)
                targetElement = kv.getKey();

            assert targetElement != null;
            holder.registerProblem(
                    targetElement,
                    unsupportedAction.getInvalidMessage()
            );
        }
    }
}
