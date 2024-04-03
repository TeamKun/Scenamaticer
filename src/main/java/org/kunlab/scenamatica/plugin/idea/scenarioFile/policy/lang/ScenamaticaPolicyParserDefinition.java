package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLParserDefinition;

public class ScenamaticaPolicyParserDefinition extends YAMLParserDefinition
{
    private static final IFileElementType TYPE = new IFileElementType(ScenamaticaPolicyLanguage.INSTANCE);

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider)
    {
        return new ScenamaticaPolicyFile(viewProvider);
    }

    @Override
    public @NotNull IFileElementType getFileNodeType()
    {
        return TYPE;
    }
}
