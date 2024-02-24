package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.RefsBrowserWindow;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.WebReference;

public class ActionReferences
{
    public static void navigate(Project project, Editor editor, String actionName)
    {
        String reference = WebReference.actionToWebReference(actionName);
        if (reference == null)
        {
            showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToResolve"));
            return;
        }

        RefsBrowserWindow window = RefsBrowserWindow.getCurrentWindow(project);
        if (window == null)
        {
            showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToFindWindow"));
            return;
        }

        window.navigateTo(reference);
    }

    private static void showErrorMessage(@NotNull Editor editor, @NotNull String message)
    {
        ApplicationManager.getApplication().invokeLater(() -> {
            HintManager.getInstance().showErrorHint(
                    editor,
                    message
            );
        });
    }
}
