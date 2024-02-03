package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.Getter;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class RefsBrowserWindow implements Disposable
{
    @Getter
    private final Project project;
    private final ToolWindow window;
    private final JBCefBrowser browser;
    @Getter
    private JPanel panel;
    private JTextField tbURL;
    private BrowserPanel browserPanel;

    private String currentURL;
    private boolean isLoad;

    static
    {
        injectPoolSize();
    }

    public RefsBrowserWindow(Project project, ToolWindow window)
    {
        this.project = project;
        this.window = window;
        this.browser = JBCefBrowser.createBuilder()
                .setCreateImmediately(true)
                .setMouseWheelEventEnable(true)
                .build();
        this.browser.getJBCefClient().addRequestHandler(new URLRequestHandler(), this.browser.getCefBrowser());

        JComponent component = this.browser.getComponent();
        this.browserPanel.add(component);
        // Patch scrolling
        patchScrolling(this.browser);

        this.navigateTo("https://scenamatica.kunlab.org/references");
    }

    private void patchURLBar()
    {
        this.tbURL.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                RefsBrowserWindow.this.tbURL.selectAll();
            }
        });

        // Change cursor
        this.tbURL.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    private void injectURLBar(JBCefBrowser browser)
    {
        JBCefJSQuery query = JBCefJSQuery.create((JBCefBrowserBase) browser);
        query.addHandler(link -> {
            this.tbURL.setText(link);
            return null;
        });

        browser.getCefBrowser().executeJavaScript(
                "navigation.addEventListener('navigate', function(e) { "
                        + query.inject("e.destination.url")
                        + "});",
                browser.getCefBrowser().getURL(),
                0
        );
    }

    private void injectURLBarHandler(JBCefBrowser browser)
    {
        JBCefJSQuery query = JBCefJSQuery.create((JBCefBrowserBase) browser);
        query.addHandler(link -> {
            BrowserUtil.browse(link);
            return null;
        });

        browser.getCefBrowser().executeJavaScript(
                "navigation.addEventListener('navigate', function(e) { "
                        + query.inject("e.destination.url")
                        + "});",
                browser.getCefBrowser().getURL(),
                0
        );
    }

    private void createUIComponents()
    {
        this.browserPanel = new BrowserPanel(this);
        this.browserPanel.setLayout(new BoxLayout(this.browserPanel, BoxLayout.Y_AXIS));
    }

    public void navigateTo(String url)
    {
        this.isLoad = false;
        this.currentURL = url;
        this.tbURL.setText(url);

        if (this.isOpen())
            this.onOpen();
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

    private static void injectPoolSize()
    {
        RegistryValue value = Registry.get("ide.browser.jcef.jsQueryPoolSize");
        if (value.asInteger() == 0)
            value.setValue(5);
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
                    e.getModifiersEx(),
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
            for (int i = 0; i < panel.getComponentCount(); i++)
            {
                if (panel.getComponent(i) instanceof BrowserPanel)
                    return ((BrowserPanel) panel.getComponent(i)).getBrowser();
            }
        }

        return null;
    }

    public static RefsBrowserWindow getCurrentWindow(Project proj)
    {
        ToolWindow window = ToolWindowManager.getInstance(proj).getToolWindow("Scenamatica");
        if (window == null)
            return null;

        return getCurrentWindow(window);
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

    private class URLRequestHandler extends CefRequestHandlerAdapter
    {
        @Override
        public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture, boolean isRedirect)
        {
            String url = request.getURL();
            if (!url.startsWith("https://scenamatica.kunlab.org"))
            {
                BrowserUtil.browse(url);
                return true;
            }

            if (!RefsBrowserWindow.this.tbURL.getText().equals(url))
                RefsBrowserWindow.this.tbURL.setText(url);

            // URL バーの同期と外部リンクのインジェション
            JBCefBrowser browserComponent = JBCefBrowser.getJBCefBrowser(browser);
            assert browserComponent != null;
            ApplicationManager.getApplication().invokeLater(() -> {
                RefsBrowserWindow.this.injectURLBar(browserComponent);
            });

            return false;
        }
    }
}
