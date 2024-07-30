package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerCategory;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerReference;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.RefsBrowserWindow;

import java.util.Optional;

@SuppressWarnings("OptionalAssignedToNull")
public class ReferenceNavigator
{

    public static void navigateToActionReference(@NotNull Project project, @NotNull Editor editor, LedgerAction actionDefinition)
    {
        String reference = "";

        Optional<LedgerCategory> category = getCategory(editor, actionDefinition.getCategory());
        if (category == null)
            return;
        else if (category.isPresent())
            reference += category.get().getId() + "/";

        reference += actionDefinition.getId();
        reference += "#action";

        navigate(project, editor, NavigateType.ACTION, reference);
    }

    @Nullable
    private static Optional<LedgerCategory> getCategory(@NotNull Editor editor, @Nullable LedgerReference ref)
    {
        if (ref == null)
            return Optional.empty();
        else if (!ref.canResolve())
        {
            showUnableToResolveErrorMessage(editor);
            return null;
        }


        Optional<LedgerCategory> category = LedgerManagerService.getInstance().resolveReference(ref, LedgerCategory.class);
        if (category.isEmpty())
        {
            showUnableToResolveErrorMessage(editor);
            return null;
        }

        return category;
    }

    private static void showUnableToResolveErrorMessage(@NotNull Editor editor)
    {
        showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToResolve"));
    }

    private static void navigate(@NotNull Project project, @NotNull Editor editor, @NotNull ReferenceNavigator.NavigateType navigateType, String componentName)
    {
        if (componentName == null)
        {
            showUnableToResolveErrorMessage(editor);
            return;
        }

        String reference = navigateType.getUrl() + componentName;
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

    public static void navigateToTypeReference(Project project, Editor editor, LedgerType typeDefinition)
    {
        String reference = "";
        Optional<LedgerCategory> category = getCategory(editor, typeDefinition.getCategory());
        if (category == null)
            return;
        else if (category.isPresent())
            reference += category.get().getId() + "/";

        reference += typeDefinition.getId();
        reference += "#type";

        navigate(project, editor, NavigateType.TYPE, reference);
    }

    @Getter
    public enum NavigateType
    {
        ACTION("/references/actions/"),
        TYPE("/references/types/"),

        DOC("/docs/");

        public static final String URL_BASE = "https://scenamatica.kunlab.org";

        private final String url;

        NavigateType(String path)
        {
            this.url = URL_BASE + path;
        }
    }
}
