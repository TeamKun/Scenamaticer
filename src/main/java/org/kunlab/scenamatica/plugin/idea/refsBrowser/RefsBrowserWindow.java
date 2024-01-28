package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.jcef.JBCefBrowser;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.Getter;

import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class RefsBrowserWindow implements Disposable
{
    private final Project project;
    private final ToolWindow window;
    private final JBCefBrowser browser;
    @Getter
    private final BrowserPanel panel;

    private String currentURL;
    private boolean isLoad;

    public RefsBrowserWindow(Project project, ToolWindow window)
    {
        this.project = project;
        this.window = window;
        this.browser = JBCefBrowser.createBuilder()
                .setCreateImmediately(true)
                .setMouseWheelEventEnable(true).build();
        this.panel = new BrowserPanel(this);
        this.panel.setPreferredSize(new Dimension(900, 600));

        JComponent component = this.browser.getComponent();
        this.panel.add(component);
        // Align fill
        this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.X_AXIS));
        // Patch scrolling
        patchScrolling(this.browser);

        this.browser.setOpenLinksInExternalBrowser(true);
        this.navigateTo("https://scenamatica.kunlab.org/references");
    }

    public void navigateTo(String url)
    {
        this.isLoad = false;
        this.currentURL = url;
    }

    public void onOpen()
    {
        if (this.isLoad || this.currentURL == null)
            return;

        this.isLoad = true;
        this.browser.loadURL(this.currentURL);
    }

    @Override
    public void dispose()
    {
        this.browser.dispose();
    }

    public void show()
    {
        this.window.show();
    }

    public boolean isOpen()
    {
        return this.window.isVisible();
    }

    public void hide()
    {
        this.window.hide();
    }

    private static void patchScrolling(JBCefBrowser browser)
    {
        AtomicBoolean isScrolling = new AtomicBoolean(false);
        browser.getCefBrowser().getUIComponent().addMouseWheelListener(e -> {
            if (isScrolling.get())
                return;
            MouseWheelEvent event = new MouseWheelEvent(
                    browser.getCefBrowser().getUIComponent(),
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers(),
                    e.getX(),
                    e.getY(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getScrollType(),
                    e.getScrollAmount(),
                    e.getWheelRotation()
            );
            isScrolling.set(true);
            for (int i = 0; i < 4; i++)
                browser.getCefBrowser().getUIComponent().dispatchEvent(event);
            isScrolling.set(false);
        });
    }

    public static RefsBrowserWindow getCurrentWindow(ToolWindow window)
    {
        if (window == null)
            return null;

        for (Content content : window.getContentManager().getContents())
        {
            JPanel panel = (JPanel) content.getComponent();
            if (!(panel instanceof RefsBrowserWindow.BrowserPanel))
                continue;

            return ((RefsBrowserWindow.BrowserPanel) panel).getBrowser();
        }

        return null;
    }

    @Getter
    public static class BrowserPanel extends JPanel
    {
        private final RefsBrowserWindow browser;

        public BrowserPanel(RefsBrowserWindow browser)
        {
            this.browser = browser;
        }
    }
}
