package org.kunlab.scenamatica.plugin.idea.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class ScenarioFiles
{
    public static final String KEY_SCENAMATICA = "scenamatica";

    public static boolean isScenarioFile(Project proj, VirtualFile file)
    {
        if (file == null || file.isDirectory() || !file.isValid())
            return false;

        if (!ApplicationManager.getApplication().isReadAccessAllowed())
            return false;

        PsiFile psiFile = PsiUtilCore.getPsiFile(proj, file);
        if (!(psiFile.getFileType() instanceof YAMLFileType))
            return false;

        return isScenarioFile((YAMLFile) psiFile);
    }

    public static boolean isScenarioFile(YAMLFile yaml)
    {
        if (!ApplicationManager.getApplication().isReadAccessAllowed())
            return false;

        for (YAMLKeyValue keyValue : YAMLUtil.getTopLevelKeys(yaml))
        {
            if (keyValue.getKeyText().equals(KEY_SCENAMATICA))
                return true;
        }

        return false;
    }
}
