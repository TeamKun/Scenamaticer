package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

public class ScenarioFileOpenListener implements FileEditorManagerListener
{
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file)
    {
        if (!ScenamaticerSettingsState.getInstance().isRefsWindowAutoOpen())
            return;

        RefsBrowserWindow window = getCurrentWindow(source.getProject());
        if (window == null || window.isOpen())
            return;

        if (ScenarioFiles.isScenarioFile(source.getProject(), file))
            window.show();
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event)
    {

        RefsBrowserWindow window = getCurrentWindow(event.getManager().getProject());
        if (window == null)
            return;

        VirtualFile newFile = event.getNewFile();
        boolean isScenarioFile = ScenarioFiles.isScenarioFile(event.getManager().getProject(), newFile);
        if (!isScenarioFile && window.isOpen())
        {
            if (ScenamaticerSettingsState.getInstance().isRefsWindowAutoClose())
                window.hide();
        }
        else if (isScenarioFile && !window.isOpen())
        {
            if (ScenamaticerSettingsState.getInstance().isRefsWindowAutoOpen())
                window.show();
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file)
    {
        if (!ScenamaticerSettingsState.getInstance().isRefsWindowAutoClose())
            return;

        RefsBrowserWindow window = getCurrentWindow(source.getProject());
        if (window == null || !window.isOpen())
            return;

        window.hide();
    }

    private static RefsBrowserWindow getCurrentWindow(Project proj)
    {
        ToolWindow window = ToolWindowManager.getInstance(proj).getToolWindow("Scenamatica");
        if (window == null)
            return null;

        return RefsBrowserWindow.getCurrentWindow(window);
    }
}
