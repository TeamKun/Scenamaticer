package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public abstract class AbstractScenamaticaInspection extends LocalInspectionTool
{
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final HighlightDisplayLevel defaultLevel;
    private HighlightDisplayKey key;

    public AbstractScenamaticaInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        this.id = id;
        this.displayName = displayName;
        this.defaultLevel = defaultLevel;
    }

    @Override
    public @NonNls @NotNull String getID()
    {
        return this.id;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return true;
    }

    protected static TextRange keyTextRangeOf(@NotNull YAMLKeyValue kv)
    {
        PsiElement key = kv.getKey();
        if (key == null)
            return TextRange.EMPTY_RANGE;

        return key.getTextRange();
    }
}
