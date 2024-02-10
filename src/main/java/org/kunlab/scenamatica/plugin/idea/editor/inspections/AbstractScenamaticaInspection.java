package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractScenamaticaInspection extends LocalInspectionTool
{
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final HighlightDisplayLevel defaultLevel;

    public AbstractScenamaticaInspection(String id, String displayName, HighlightDisplayLevel defaultLevel)
    {
        this.id = id;
        this.displayName = displayName;
        this.defaultLevel = defaultLevel;
    }

    @Override
    public @NonNls @NotNull String getID()
    {
        return "Scenamatica:" + this.id;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return true;
    }
}
