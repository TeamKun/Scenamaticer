package org.kunlab.scenamatica.plugin.idea.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class ScenarioFiles
{
    public static final String KEY_SCENAMATICA = "scenamatica";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";

    public static boolean isScenarioFile(Project proj, VirtualFile file)
    {
        PsiFile psiFile = YAMLUtils.toPSIFile(proj, file);
        if (psiFile == null || !(psiFile.getFileType() instanceof YAMLFileType))
            return false;

        return isScenarioFile((YAMLFile) psiFile);
    }

    public static boolean isScenarioFile(PsiFile file)
    {
        if (!(file.getFileType() instanceof YAMLFileType))
            return false;

        return isScenarioFile((YAMLFile) file);
    }

    public static boolean isScenarioFile(YAMLFile yaml)
    {
        for (YAMLKeyValue keyValue : YAMLUtil.getTopLevelKeys(yaml))
        {
            if (keyValue.getKeyText().equals(KEY_SCENAMATICA))
                return true;
        }

        return false;
    }

}
