package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import javax.swing.Icon;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenamaticaIcons;

import java.io.InputStream;

public class ScenarioFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile
{
    public static final ScenarioFileType INSTANCE = new ScenarioFileType();
    private static final char[] MARKER = "scenamatica: ".toCharArray();
    private static final String[] EXTS = {"yml", "yaml"};

    private ScenarioFileType()
    {
        super(ScenarioFileLanguage.INSTANCE);
    }

    @Override
    public @NonNls @NotNull String getName()
    {
        return "Scenamatica Scenario File";
    }

    @Override
    public @NotNull String getDescription()
    {
        return "Scenamatica Scenario File";
    }

    @Override
    public @NotNull String getDefaultExtension()
    {
        return "";
    }

    @Override
    public Icon getIcon()
    {
        return ScenamaticaIcons.ICON;
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile virtualFile)
    {
        // 拡張子が違うものを弾く
        String ext = virtualFile.getExtension();
        if (!ArrayUtils.contains(EXTS, ext))
            return false;

        // scenamatica: がトップにあるかどうか判定する
        try (InputStream stream = virtualFile.getInputStream())
        {
            byte[] buffer = new byte[256];
            boolean isAfterNewLine = true;
            int currentMarkerIndex = 0;  // 現在のマーカー文字列のn文字目
            while (stream.read(buffer) != -1)
            {
                for (byte b : buffer)
                {
                    char c = (char) b;
                    if (c == '\r' || c == '\n')
                    {
                        isAfterNewLine = true;  // 改行文字が来たら次の文字はマーカーの先頭文字である。
                        continue;
                    }

                    if (isAfterNewLine && c != MARKER[0])
                    {
                        isAfterNewLine = false;
                        continue;
                    }

                    if (c == MARKER[currentMarkerIndex])
                    {
                        currentMarkerIndex++;
                        if (currentMarkerIndex == MARKER.length)
                            return true;
                    }
                    else
                        currentMarkerIndex = 0;

                    isAfterNewLine = false;
                }
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return false;
    }

    public static boolean isType(@Nullable PsiFile file)
    {
        return file != null && file.getFileType() == INSTANCE;
    }

    public static boolean isType(@Nullable VirtualFile file)
    {
        return file != null && file.getFileType() == INSTANCE;
    }
}
