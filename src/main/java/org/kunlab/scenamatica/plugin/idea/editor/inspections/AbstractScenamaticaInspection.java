package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
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
        return "Scenamatica:" + this.id;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return true;
    }

    public HighlightDisplayLevel getHighlightLevel(PsiElement elm)
    {
        if (this.key == null)
            this.key = HighlightDisplayKey.findOrRegister(this.getShortName(), this.getDisplayName(), this.getID());

        return InspectionProfileManager.getInstance().getCurrentProfile().getErrorLevel(this.key, elm);
    }

    public ProblemHighlightType getHighlightType(PsiElement elm)
    {
        HighlightDisplayLevel level = this.getHighlightLevel(elm);
        if (level == HighlightDisplayLevel.ERROR)
            return ProblemHighlightType.ERROR;
        else if (level == HighlightDisplayLevel.WARNING)
            return ProblemHighlightType.WARNING;
        else if (level == HighlightDisplayLevel.WEAK_WARNING)
            return ProblemHighlightType.WEAK_WARNING;
        else if (level == HighlightDisplayLevel.DO_NOT_SHOW)
            return ProblemHighlightType.INFORMATION;
        else
            return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }
}
