package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerScenarioResolver;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractScenarioFileInspection extends AbstractScenamaticaInspection
{
    public AbstractScenarioFileInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        super(id, displayName, defaultLevel);
    }

    protected abstract void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session);

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session)
    {
        return new PsiElementVisitor()
        {
            @Override
            public void visitElement(@NotNull PsiElement element)
            {
                if (element instanceof ScenarioFile file)
                    visitScenarioFile(file, holder, session);
            }
        };

    }

    protected void reportDetailedResolveErrorTypeOf(@NotNull ScenarioFile file,
                                                    @NotNull ProblemsHolder holder,
                                                    @NotNull LocalInspectionToolSession session,
                                                    @Nullable Function<? super List<LedgerScenarioResolver.ResolveResult>, ? extends LocalQuickFix> fixSupply,
                                                    @NotNull LedgerScenarioResolver.ResolveResult.InvalidCause... causesOf)
    {
        LedgerScenarioResolver resolveResults = LedgerScenarioResolver.create(
                LedgerManagerService.getInstance(),
                file,
                session
        ).detailedResolve();

        List<LedgerScenarioResolver.ResolveResult> unsupportedActions =
                resolveResults.getErrors(causesOf);

        if (unsupportedActions.isEmpty())
            return;

        LocalQuickFix fix = fixSupply == null ? null: fixSupply.apply(unsupportedActions);

        for (LedgerScenarioResolver.ResolveResult unsupportedAction : unsupportedActions)
        {
            PsiElement targetElement = unsupportedAction.getElement();
            if (targetElement instanceof YAMLKeyValue kv)
                targetElement = kv.getKey();

            assert targetElement != null;
            if (fix != null)
                holder.registerProblem(
                        targetElement,
                        unsupportedAction.getInvalidMessage(),
                        fix
                );
            else
                holder.registerProblem(
                        targetElement,
                        unsupportedAction.getInvalidMessage()
                );
        }
    }
}
