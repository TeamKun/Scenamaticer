package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.jetbrains.annotations.NotNull;

public class BrowserOpenListener implements ToolWindowManagerListener
{
    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager, @NotNull ToolWindowManagerListener.ToolWindowManagerEventType changeType)
    {
        if (changeType != ToolWindowManagerEventType.ActivateToolWindow)
            return;

        ToolWindow window = toolWindowManager.getToolWindow("Scenamatica");
        if (window == null)
            return;

        RefsBrowserWindow browserWindow = RefsBrowserWindow.getCurrentWindow(window);
        if (browserWindow == null)
            return;

        browserWindow.onOpen();
    }
}
