package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFileType;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

public class ScenarioFileOpenListener implements FileEditorManagerListener
{
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file)
    {
        if (!ScenamaticerSettingsState.getInstance().isRefsWindowAutoOpen())
            return;

        RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(source.getProject());
        if (window == null || window.isOpen())
            return;

        if (ScenarioFileType.isType(file))
            window.show();
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event)
    {

        RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(event.getManager().getProject());
        if (window == null)
            return;

        VirtualFile newFile = event.getNewFile();
        if (newFile == null)
            return;

        boolean isScenarioFile = ScenarioFileType.isType(newFile);
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

        RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(source.getProject());
        if (window == null || !window.isOpen())
            return;

        window.hide();
    }

}
