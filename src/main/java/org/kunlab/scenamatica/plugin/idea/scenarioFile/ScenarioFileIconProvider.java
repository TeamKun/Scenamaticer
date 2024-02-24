package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

public class ScenarioFileIconProvider implements FileIconProvider
{
    public static final Icon ICON = IconLoader.getIcon("/icons/scenario.png", ScenarioFileIconProvider.class);
    public static final Icon SCENAMATICA_ICON = IconLoader.getIcon("/icons/scenamatica.png", ScenarioFileIconProvider.class);
    public static final Icon ACTION_ICON = IconLoader.getIcon("/icons/action.png", ScenarioFileIconProvider.class);

    @Override
    public @Nullable Icon getIcon(@NotNull VirtualFile virtualFile, int i, @Nullable Project project)
    {
        if (project == null)
            return null;

        // Check if the element is a yaml file and has "scenamatica" in the file
        // If so, return the scenamatica icon

        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        if (file == null || !(file.getFileType() instanceof YAMLFileType))
            return null;

        // Check if the file is a scenamatica file
        if (ScenarioFiles.isScenarioFile((YAMLFile) file))
            return ICON;
        else
            return null;
    }
}
