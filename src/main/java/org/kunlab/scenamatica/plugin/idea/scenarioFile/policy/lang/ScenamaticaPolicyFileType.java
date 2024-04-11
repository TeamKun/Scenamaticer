package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenamaticaIcons;

public class ScenamaticaPolicyFileType extends LanguageFileType
{
    public static final ScenamaticaPolicyFileType INSTANCE = new ScenamaticaPolicyFileType();

    public static final String EXTENSION = "scenamatica";
    public static final String FULL_NAME = "." + EXTENSION;

    private ScenamaticaPolicyFileType()
    {
        super(ScenamaticaPolicyLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName()
    {
        return "Scenamatica Policy File";
    }

    @Override
    public @NotNull String getDescription()
    {
        return "Scenamatica Policy File";
    }

    @Override
    public @NotNull String getDefaultExtension()
    {
        return EXTENSION;
    }

    @Override
    public Icon getIcon()
    {
        return ScenamaticaIcons.SCENAMATICA_ICON;
    }
}
