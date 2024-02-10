package org.kunlab.scenamatica.plugin.idea.editor.fixes;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLQuotedText;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class ValueIncrementalFix extends LocalQuickFixAndIntentionActionOnPsiElement
{
    private static final Pattern PATTERN_IDX = Pattern.compile("_(\\d+)$");

    private final String scenarioName;
    private final int index;
    private final String incrementedName;

    public ValueIncrementalFix(PsiElement element, String scenarioName)
    {
        super(element);
        this.scenarioName = scenarioName;
        this.index = getIncrementalIndex(scenarioName);
        this.incrementedName = getIncrementedName(scenarioName, this.index);
    }

    @Override
    public @IntentionName @NotNull String getText()
    {
        return ScenamaticerBundle.of("editor.fixes.valueIncremental.title", this.incrementedName);
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName()
    {
        return "Scenamatica";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor, @NotNull PsiElement element, @NotNull PsiElement ignored)
    {
        if (this.isAvailable(project, editor, file))
        {
            LOG.assertTrue(file == element.getContainingFile());

            int start = element.getTextRange().getStartOffset();
            int end = element.getTextRange().getEndOffset();
            assert editor != null;

            // check quoted
            if (element.getParent() instanceof YAMLQuotedText)
            {
                start++;
                end--;
            }

            editor.getDocument().replaceString(start, end, this.incrementedName);
        }
    }

    @Override
    public boolean startInWriteAction()
    {
        return true;
    }

    private static String getIncrementedName(String name, int index)
    {
        Matcher matcher = PATTERN_IDX.matcher(name);
        if (matcher.find())
            return name.substring(0, matcher.start(1)) + index;

        return name + "_" + index;
    }

    private static int getIncrementalIndex(String name)
    {
        Matcher matcher = PATTERN_IDX.matcher(name);
        if (matcher.find())
            return Integer.parseInt(matcher.group(1)) + 1;

        return 2;
    }
}
