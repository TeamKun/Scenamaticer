package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.elements;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileElement;

public class ElementOn implements ScenarioFileElement
{
    @Override
    public String getApplicableKeys()
    {
        return "on";
    }

    @Override
    public PsiReference resolveReference(@NotNull PsiElement element)
    {
        return null;
    }
}
