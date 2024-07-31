package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenamaticaIcons;

public class ScenamaticaPolicyFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile
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

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file)
    {
        return FileTypeRegistry.getInstance().isFileOfType(file, YAMLFileType.YML)
                && FileUtil.namesEqual(file.getName(), ScenamaticaPolicyFileType.FULL_NAME);
    }
}
