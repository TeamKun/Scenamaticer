package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public interface ScenarioFileElement
{
    String getApplicableKeys();

    PsiReference resolveReference(@NotNull PsiElement element);
}
