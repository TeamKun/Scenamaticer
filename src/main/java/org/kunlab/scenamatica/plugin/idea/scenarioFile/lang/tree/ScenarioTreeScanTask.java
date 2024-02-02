package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.tree;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;

import java.util.ArrayList;
import java.util.List;

public class ScenarioTreeScanTask extends Task.Backgroundable
{
    public ScenarioTreeScanTask(@NotNull Project project)
    {
        super(project, "Scanning for scenarios", false);
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator)
    {
        this.scanAllScenarios(this.getProject(), progressIndicator);
    }

    @SneakyThrows
    private void scanAllScenarios(Project project, ProgressIndicator indicator)
    {
        indicator.setText("Scanning  all scenarios...");
        indicator.setText2("Collecting all scenario files...");
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        List<VirtualFile> allFiles = new ArrayList<>();
        for (Module mod : moduleManager.getModules())
        {
            ModuleRootManager rootMgr = ModuleRootManager.getInstance(mod);
            for (VirtualFile root : rootMgr.getSourceRoots(JavaResourceRootType.RESOURCE))
                allFiles.addAll(collectScenarioFiles(project, root));
        }

        for (int i = 0; i < allFiles.size(); i++)
        {
            VirtualFile file = allFiles.get(i);
            indicator.setFraction((double) i / allFiles.size());
            indicator.setText2("Scanning " + file.getPresentableUrl());
            scanOneFile(project, file);
        }
    }

    private List<VirtualFile> collectScenarioFiles(Project project, VirtualFile root)
    {
        List<VirtualFile> scenarioFiles = new ArrayList<>();

        VfsUtilCore.iterateChildrenRecursively(
                root,
                null,
                (file) -> {
                    if (ScenarioFiles.isScenarioFile(project, file))
                        scenarioFiles.add(file);
                    return true;
                }
        );

        return scenarioFiles;
    }

    private void scanOneFile(Project project, VirtualFile file)
    {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null)
            return;
        assert psiFile instanceof YAMLFile;

        YAMLUtil.getTopLevelKeys((YAMLFile) psiFile).forEach(
                ScenarioTrees::embedKeyAll
        );
    }
}
