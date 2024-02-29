package org.kunlab.scenamatica.plugin.idea.editor.fixes;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

import java.util.Arrays;
import java.util.List;

public class DeleteElementFix extends LocalQuickFixAndIntentionActionOnPsiElement
{
    private final List<SmartPsiElementPointer<PsiElement>> elements;

    public DeleteElementFix(PsiElement... elements)
    {
        super(elements[0], elements[elements.length - 1]);
        this.elements = Arrays.stream(elements)
                .map(SmartPointerManager::createPointer)
                .toList();
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor, @NotNull PsiElement psiElement, @NotNull PsiElement psiElement1)
    {
        if (this.isAvailable(project, editor, psiFile))
        {
            for (SmartPsiElementPointer<PsiElement> element : this.elements)
            {
                PsiElement resolved = element.getElement();
                if (resolved == null)
                    continue;
                assert editor != null;
                int line = editor.getDocument().getLineNumber(resolved.getTextOffset());
                resolved.delete();
                if (resolved instanceof YAMLKeyValue)
                    removeLine(line, editor);
            }
        }
    }

    @Override
    public @IntentionName @NotNull String getText()
    {
        StringBuilder presentableTexts = new StringBuilder();
        for (SmartPsiElementPointer<PsiElement> element : this.elements)
        {
            PsiElement resolved = element.getElement();
            if (!presentableTexts.isEmpty())
                presentableTexts.append(", ");
            presentableTexts.append("'").append(getPresentableText(resolved)).append("'");
        }

        return ScenamaticerBundle.of("editor.fixes.deleteElement.title", presentableTexts.toString());
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName()
    {
        return "Scenamatica";
    }

    private static void removeLine(int line, Editor editor)
    {
        Document document = editor.getDocument();
        int startOffset = document.getLineStartOffset(line);
        int endOffset = document.getLineEndOffset(line) + "\n".length();
        document.deleteString(startOffset, endOffset);
    }

    private static String getPresentableText(PsiElement element)
    {
        if (element instanceof YAMLKeyValue)
            return ((YAMLKeyValue) element).getKeyText();
        return element.getText();
    }
}
