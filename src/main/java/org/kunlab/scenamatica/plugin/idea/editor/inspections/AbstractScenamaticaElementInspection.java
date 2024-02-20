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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Iterator;

public abstract class AbstractScenamaticaElementInspection extends AbstractScenamaticaInspection
{
    public AbstractScenamaticaElementInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        super(id, displayName, defaultLevel);
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
            if (!(el instanceof YAMLKeyValue))
                continue;
            boolean doContinue = visitYamlKV((YAMLKeyValue) el, holder);
            if (!doContinue)
                break;
        }
    }

    protected abstract boolean visitYamlKV(@NotNull YAMLKeyValue kv, @NotNull ProblemsHolder holder);
}
