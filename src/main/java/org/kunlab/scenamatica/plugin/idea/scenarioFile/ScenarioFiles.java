package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.nio.file.Path;
import java.util.List;

public class ScenarioFiles
{
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";

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
