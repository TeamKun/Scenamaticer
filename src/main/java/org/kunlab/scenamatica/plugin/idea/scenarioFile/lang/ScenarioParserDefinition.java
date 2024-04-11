package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLParserDefinition;

public class ScenarioParserDefinition extends YAMLParserDefinition
{
    private static final IFileElementType TYPE = new IFileElementType(ScenarioFileLanguage.INSTANCE);

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider)
    {
        return new ScenarioFile(viewProvider);
    }

    @Override
    public @NotNull IFileElementType getFileNodeType()
    {
        return TYPE;
    }
}
