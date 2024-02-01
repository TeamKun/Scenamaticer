package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.nio.file.Path;
import java.util.List;

public class ScenarioFiles
{
    public static final String KEY_SCENAMATICA = "scenamatica";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";

    public static boolean isScenarioFile(Project proj, VirtualFile file)
    {
        if (file.getFileType() != YAMLFileType.YML)
            return false;

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

    public static String toRelativePath(Project project, String fullScenarioFilePath)
    {
        Path projectPath;
        if (project.getBasePath() != null)
            projectPath = Path.of(project.getBasePath());
        else
            projectPath = Path.of("");

        Path scenarioPath = Path.of(fullScenarioFilePath);

        // Get Project's all resources root
        for (Module mod : ModuleManager.getInstance(project).getModules())
        {
            ModuleRootManager rootMgr = ModuleRootManager.getInstance(mod);
            List<VirtualFile> resourceRoots = rootMgr.getSourceRoots(JavaResourceRootType.RESOURCE);

            for (VirtualFile resourceRoot : resourceRoots)
            {
                Path resourcePath = resourceRoot.toNioPath();
                if (scenarioPath.startsWith(resourcePath))
                    return projectPath.relativize(scenarioPath).toString();
            }
        }

        if (scenarioPath.startsWith(projectPath))
            return projectPath.relativize(scenarioPath).toString();

        return fullScenarioFilePath;
    }
}
