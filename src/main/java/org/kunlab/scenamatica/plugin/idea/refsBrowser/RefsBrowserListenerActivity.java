package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RefsBrowserListenerActivity implements ProjectActivity
{
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation)
    {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new ScenarioFileOpenListener());
        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new BrowserOpenListener());
        ApplicationManager.getApplication().invokeLater(() -> {
            RefsBrowserWindow browserWindow = RefsBrowserWindow.getCurrentWindow(ToolWindowManager.getInstance(project).getToolWindow("Scenamatica"));
            if (browserWindow != null && browserWindow.isOpen())
                browserWindow.onOpen();
        });
        return null;
    }
}
