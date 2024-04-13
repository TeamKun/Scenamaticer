package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Iterator;

public abstract class AbstractScenarioFileInspection extends AbstractScenamaticaInspection
{
    public AbstractScenarioFileInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        super(id, displayName, defaultLevel);
    }

    @Override
    public ProblemDescriptor @Nullable [] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly)
    {
        if (!(file instanceof ScenarioFile))
            return ProblemDescriptor.EMPTY_ARRAY;


        return ProgressManager.getInstance().runProcess(
                () -> ApplicationManager.getApplication().runReadAction((ThrowableComputable<ProblemDescriptor[], ProcessCanceledException>) () -> {
                    if (file.getProject().isDisposed())
                        return ProblemDescriptor.EMPTY_ARRAY;
                    else
                    {
                        ProblemsHolder holder = new ProblemsHolder(manager, file, isOnTheFly);
                        visitScenarioFile((ScenarioFile) file, holder);

                        Key<?>[] tempFileKeys = getTempFileKeys();
                        if (tempFileKeys != null)
                        {
                            for (Key<?> key : tempFileKeys)
                                file.putUserData(key, null);
                        }

                        return holder.getResultsArray();
                    }
                }),
                new EmptyProgressIndicator()
        );
    }

    private void visitScenarioFile(@NotNull ScenarioFile file, @NotNull ProblemsHolder holder)
    {
        Iterator<PsiElement> it = YAMLUtils.getDepthFirstIterator(file);
        while (it.hasNext())
        {
            PsiElement el = it.next();
            if (!(el instanceof YAMLKeyValue))
                continue;
            boolean doContinue = visitYamlKV(file, (YAMLKeyValue) el, holder);
            if (!doContinue)
                break;
        }
    }

    protected abstract boolean visitYamlKV(@NotNull ScenarioFile file, @NotNull YAMLKeyValue kv, @NotNull ProblemsHolder holder);

    @Nullable
    protected Key<?>[] getTempFileKeys()
    {
        return null;
    }
}
