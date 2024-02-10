package org.kunlab.scenamatica.plugin.idea.scenarioFile.tree;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.psi.PsiManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerPluginDisposable;

public class ScenarioTreeActivity implements ProjectActivity
{
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation)
    {
        PsiManager.getInstance(project).addPsiTreeChangeListener(
                new ScenarioTreeChangeListener(),
                ScenamaticerPluginDisposable.getInstance(project)
        );

        ProgressManager.getInstance().run(new ScenarioTreeScanTask(project));
        return null;
    }

}
