package org.kunlab.scenamatica.plugin.idea.editor;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFileIconProvider;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

public class ActionReferenceLineMarkerProvider implements LineMarkerProvider
{
    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement)
    {
        if (!acceptsElement(psiElement))
            return null;


        SchemaResolver.ScenarioAction scenarioAction = SchemaProviderService.getResolver().getAction(psiElement);
        if (scenarioAction == null)
            return null;

        SchemaAction action = SchemaProviderService.getProvider().getAction(scenarioAction.getName());
        if (action == null)
            return null;

        Editor editor = FileEditorManager.getInstance(psiElement.getProject()).getSelectedTextEditor();
        return new LineMarkerInfo<>(
                scenarioAction.getActionName(),
                psiElement.getTextRange(),
                ScenarioFileIconProvider.ACTION_ICON,
                psiElement1 -> ScenamaticerBundle.of("editor.lineMarkers.action.tooltip", action.name()),
                (e, s) -> {
                    ActionReferences.navigate(psiElement.getProject(), editor, action.name());
                },
                GutterIconRenderer.Alignment.LEFT,
                () -> "awd"
        );
    }

    private static boolean acceptsElement(@NotNull PsiElement psiElement)
    {
        String type = SchemaProviderService.getResolver().getTypeName(psiElement);
        return psiElement instanceof YAMLMapping && ("scenario".equals(type) || "action".equals(type));
    }
}
