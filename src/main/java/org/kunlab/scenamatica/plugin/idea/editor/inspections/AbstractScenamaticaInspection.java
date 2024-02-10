package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.util.ResourceUtil;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

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
        return "scenamatica:" + this.id;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return true;
    }

    @Override
    public @Nullable @Nls String getStaticDescription()
    {
        return getLocalizedDescription(ScenamaticerBundle.getCurrentLocale());
    }

    private @Nullable @Nls String getLocalizedDescription(Locale locale)
    {
        if (locale == Locale.ENGLISH)
            return super.getStaticDescription();

        String folderName = "inspectionDescriptions@" + locale.getLanguage();
        String fileName = this.getShortName() + ".html";

        try (InputStream stream = ResourceUtil.getResourceAsStream(AbstractScenamaticaInspection.class.getClassLoader(), folderName, fileName))
        {
            if (stream == null)
                return super.getStaticDescription();
            else
                return ResourceUtil.loadText(stream);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Failed to read localized inspection description", e);
        }
    }
}
