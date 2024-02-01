package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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

        Application app = ApplicationManager.getApplication();
        ProgressManager.getInstance().run(new ScenarioTreeScanTask(project));
        //app.invokeLater(() -> app.runReadAction(() -> ProgressManager.getInstance().run(new ScenarioTreeScanTask(project))));
        return null;
    }

}
