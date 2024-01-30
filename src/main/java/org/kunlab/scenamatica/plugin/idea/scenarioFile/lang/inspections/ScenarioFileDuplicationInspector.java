package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndex;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.fixes.ValueIncrementalFix;
import org.kunlab.scenamatica.plugin.idea.utils.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenarioFileDuplicationInspector extends LocalInspectionTool
{
    public static final String ID = "scenamatica:duplication";

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
    {
        return new PsiElementVisitor()
        {
            @Override
            public void visitFile(@NotNull PsiFile file)
            {
                if (!(ScenarioFiles.isScenarioFile(file) && YAMLUtils.hasValidKey(file, ScenarioFiles.KEY_NAME)))
                    return;

                PsiElement scenarioNameElement = YAMLUtils.getValue(file, ScenarioFiles.KEY_NAME);
                String scenarioName = YAMLUtils.getValueText(scenarioNameElement);
                if (scenarioName == null)
                    return;

                if (ScenarioFileIndexer.isDuplicated(holder.getProject(), scenarioName))
                    holder.registerProblem(
                            scenarioNameElement,
                            getDuplicatedText(holder.getProject(), file, scenarioName),
                            ProblemHighlightType.GENERIC_ERROR,
                            new ValueIncrementalFix(scenarioNameElement, scenarioName)
                    );
            }
        };
    }

    @Override
    public @NotNull HighlightDisplayLevel getDefaultLevel()
    {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return true;
    }

    @Override
    public @NotNull String getDisplayName()
    {
        return "ScenarioFileDuplication";
    }

    @Override
    public @NonNls @NotNull String getID()
    {
        return ID;
    }

    private static String getDuplicatedText(Project proj, PsiFile file, String dupeName)
    {
        StringBuilder sb = new StringBuilder("Scenario name \"")
                .append(dupeName)
                .append("\" is duplicated with: \n");

        for (ScenarioFileIndex path : ScenarioFileIndexer.getIndicesFor(proj, dupeName))
        {
            String filePath = file.getVirtualFile().getPath();
            if (path.path().equals(filePath))
                continue;

            sb.append("- ")
                    .append(dupeName).append("(")
                    .append(path.description().isEmpty() ? "No description provided": path.description())
                    .append(") at ")
                    .append(ScenarioFiles.toRelativePath(proj, path.path()))
                    .append("\n");
        }

        return sb.toString();
    }
}
