package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;

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
}
