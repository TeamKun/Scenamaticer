package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.settings.ScenamaticerSettingsState;

public class RefsBrowserWindowFactory implements ToolWindowFactory, DumbAware
{
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow)
    {
        if (!JBCefApp.isSupported())
        {
            JBLabel label = new JBLabel("Unable to provide Scenamatica docs: JCEF is not supported on this platform.");
            toolWindow.getContentManager().addContent(toolWindow.getContentManager().getFactory().createContent(label, "", false));
            return;
        }

        RefsBrowserWindow browserWindow = new RefsBrowserWindow(project, toolWindow);
        Content content = toolWindow.getContentManager().getFactory().createContent(
                browserWindow.getPanel(),
                "",
                false
        );

        toolWindow.getContentManager().addContent(content);
        toolWindow.setStripeTitle("Scenamatica References");
    }

    @Override
    public boolean isApplicable(@NotNull Project project)
    {
        return ScenamaticerSettingsState.getInstance().isRefsWindowEnabled();
    }
}
