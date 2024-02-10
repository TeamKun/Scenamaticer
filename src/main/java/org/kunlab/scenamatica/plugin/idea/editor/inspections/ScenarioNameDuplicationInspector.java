package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.editor.fixes.ValueIncrementalFix;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndex;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenarioNameDuplicationInspector extends AbstractScenamaticaInspection
{
    public static final String ID = "Duplication";

    public ScenarioNameDuplicationInspector()
    {
        super(ID, "DuplicatedScenarioName", HighlightDisplayLevel.ERROR);

    }

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
                            new ValueIncrementalFix(scenarioNameElement, scenarioName)
                    );
            }
        };
    }

    private static String getDuplicatedText(Project proj, PsiFile file, String dupeName)
    {
        StringBuilder sb = new StringBuilder(ScenamaticerBundle.of("editor.inspections.duplication.description.title", dupeName))
                .append("\n\n");

        for (ScenarioFileIndex path : ScenarioFileIndexer.getIndicesFor(proj, dupeName))
        {
            String filePath = file.getVirtualFile().getPath();
            if (path.path().equals(filePath))
                continue;

            sb.append("- ")
                    .append(dupeName).append("(")
                    .append(path.description().isEmpty() ? ScenamaticerBundle.of("editor.inspections.duplication.description.noDescription"): path.description())
                    .append(") at ")
                    .append(ScenarioFiles.toRelativePath(proj, path.path()))
                    .append("\n");
        }

        return sb.toString();
    }
}
