package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.fixes.ValueIncrementalFix;
import org.kunlab.scenamatica.plugin.idea.utils.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class HighlightAnnotator implements Annotator
{

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder annotationHolder)
    {
        if (!(psiElement.isValid() && ScenarioFiles.isScenarioFile(psiElement.getContainingFile())))
            return;

        processTopElement(psiElement, annotationHolder);
    }

    private static void processTopElement(PsiElement element, AnnotationHolder holder)
    {
        // check if it's a top-level key
        if (!(YAMLUtils.isValue(element) && YAMLUtils.isTopLevelKey(YAMLUtils.getKey(element))))
            return;

        String key = YAMLUtils.getKeyText(element);

        switch (key)
        {
            case ScenarioFiles.KEY_NAME:
                processScenarioName(element, holder);
                break;
            case ScenarioFiles.KEY_DESCRIPTION:
                break;
            case ScenarioFiles.KEY_SCENAMATICA:
                break;
        }
    }

    private static void processScenarioName(PsiElement element, AnnotationHolder holder)
    {
        String scenarioName = YAMLUtils.getValueText(element);
        assert scenarioName != null;
        PsiNamedElement kv = (PsiNamedElement) element.getParent().getParent();

        if (scenarioName.isEmpty())
            holder.newAnnotation(HighlightSeverity.ERROR, "Scenario name cannot be empty")
                    .range(element)
                    .create();
        else if (ScenarioFileIndexer.isDuplicated(element.getProject(), scenarioName))
            holder.newAnnotation(HighlightSeverity.ERROR, "Scenario name is duplicated")
                    .range(element)
                    .withFix(new ValueIncrementalFix(element, scenarioName))
                    .create();

        if (scenarioName.length() > 16)
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Scenario name should be less than 16 characters")
                    .range(element)
                    .create();
    }
}
