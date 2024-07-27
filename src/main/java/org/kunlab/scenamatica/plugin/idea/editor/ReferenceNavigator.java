package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLValue;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.ledger.LedgerManagerService;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerCategory;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerReference;
import org.kunlab.scenamatica.plugin.idea.refsBrowser.RefsBrowserWindow;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.Optional;

public class ReferenceNavigator
{

    @Nullable
    public static String tryGetActionNameByActionSpecifier(PsiElement element)
    {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> tryGetActionNameByActionSpecifier$0(element));
    }

    @Nullable
    private static String tryGetActionNameByActionSpecifier$0(PsiElement element)
    {
        YAMLValue yamlValue;
        if (element instanceof YAMLValue)
            yamlValue = (YAMLValue) element;
        else if (element instanceof LeafPsiElement)
        {
            PsiElement parent = element.getParent();
            if (parent instanceof YAMLValue)
                yamlValue = (YAMLValue) parent;/*
            else if (parent instanceof YAMLKeyValue)
                return element.textMatches("action");*/
            else
                return null;
        }
        else
            return null;

        YAMLPsiElement parentContainer = (YAMLPsiElement) yamlValue.getParent();
        if (!(parentContainer instanceof YAMLMapping))
            return null;

        YAMLMapping blockMapping = (YAMLMapping) parentContainer.getParent();
        if (blockMapping == null)
            return null;

        YAMLKeyValue actionKV = blockMapping.getKeyValueByKey("action");
        if (actionKV == null)
            return null;

        if (actionKV.getValue() == null)
            return null;

        return YAMLUtils.getUnquotedValueText(actionKV.getValue());
    }

    public static void navigateToActionReference(@NotNull Project project, @NotNull Editor editor, String actionName)
    {
        Optional<LedgerAction> optActionDefinition = LedgerManagerService.getInstance().getActionByID(actionName);
        if (optActionDefinition.isEmpty())
        {
            showUnableToResolveErrorMessage(editor);
            return;
        }
        LedgerAction actionDefinition = optActionDefinition.get();

        String reference = "";

        LedgerReference categoryReference = actionDefinition.getCategory();
        if (categoryReference != null)
        {
            if (!categoryReference.canResolve())
            {
                showUnableToResolveErrorMessage(editor);
                return;
            }

            Optional<LedgerCategory> category = LedgerManagerService.getInstance().resolveReference(categoryReference, LedgerCategory.class);
            if (category.isEmpty())
            {
                showUnableToResolveErrorMessage(editor);
                return;
            }
            LedgerCategory resolvedCategory = category.get();
            reference /* + */ = resolvedCategory.getId() + "/";
        }

        reference += actionDefinition.getId();

        navigate(project, editor, NavigateType.ACTION, reference);
    }

    private static void showUnableToResolveErrorMessage(@NotNull Editor editor)
    {
        showErrorMessage(editor, ScenamaticerBundle.of("editor.codeVision.actionReference.errors.unableToResolve"));
    }

    public static void navigate(@NotNull Project project, @NotNull Editor editor, @NotNull ReferenceNavigator.NavigateType navigateType, String componentName)
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

    @Getter
    public enum NavigateType
    {
        ACTION("/references/actions/"),
        TYPE("/references/types/"),

        DOC("/docs/");

        public static final String URL_BASE = "https://scenamatica.kunlab.org/";

        private final String url;

        NavigateType(String path)
        {
            this.url = URL_BASE + path;
        }
    }
}
