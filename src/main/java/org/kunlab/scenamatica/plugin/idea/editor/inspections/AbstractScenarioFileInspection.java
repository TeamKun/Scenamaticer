package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
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
            public void visitFile(@NotNull PsiFile element)
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
                                                    @Nullable Function<? super LedgerScenarioResolver.ResolveResult, ? extends PsiElement> targetElementTraverser,
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
            if (targetElementTraverser != null)
                targetElement = targetElementTraverser.apply(unsupportedAction);
            else if (targetElement instanceof YAMLKeyValue kv && kv.getKey() != null)
                targetElement = kv.getKey();

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

    protected void reportDetailedResolveErrorTypeOf(@NotNull ScenarioFile file,
                                                    @NotNull ProblemsHolder holder,
                                                    @NotNull LocalInspectionToolSession session,
                                                    @Nullable Function<? super List<LedgerScenarioResolver.ResolveResult>, ? extends LocalQuickFix> fixSupply,
                                                    @NotNull LedgerScenarioResolver.ResolveResult.InvalidCause... causesOf)
    {
        this.reportDetailedResolveErrorTypeOf(file, holder, session, fixSupply, null, causesOf);
    }

    protected PsiElement traverseKVValue(@NotNull LedgerScenarioResolver.ResolveResult mayKVHolder)
    {
        PsiElement mayKV = mayKVHolder.getElement();
        if (!(mayKV instanceof YAMLKeyValue kv))
            return mayKV;

        return kv.getValue() == null ? kv: kv.getValue();
    }

    protected PsiElement traverseKVKey(@NotNull LedgerScenarioResolver.ResolveResult mayKVHolder)
    {
        PsiElement mayKV = mayKVHolder.getElement();
        if (!(mayKV instanceof YAMLKeyValue kv))
            return mayKV;

        return kv.getKey();
    }
}
