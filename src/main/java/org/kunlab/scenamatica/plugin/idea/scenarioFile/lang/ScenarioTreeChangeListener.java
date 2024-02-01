package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;

public class ScenarioTreeChangeListener extends PsiTreeChangeAdapter
{
    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event)
    {
        long start = System.currentTimeMillis();
        if (!shouldProcess(event))
            return;

        PsiElement element = event.getChild();
        if (!(element instanceof PsiWhiteSpace || element instanceof LeafPsiElement))
            ScenarioTrees.embedKeyAll(element);

        long end = System.currentTimeMillis();
        System.out.println("ScenarioTreeChangeListener.childAdded: " + (end - start) + "ms");
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event)
    {
        if (!shouldProcess(event))
            return;

        PsiElement element = event.getChild();
        if (!(element instanceof PsiWhiteSpace || element instanceof LeafPsiElement))
            ScenarioTrees.embedKeyAll(element);
    }

    private static boolean shouldProcess(PsiTreeChangeEvent evt)
    {
        PsiFile file = evt.getFile();
        return file != null && file.isValid() && ScenarioFiles.isScenarioFile(file);
    }
}
