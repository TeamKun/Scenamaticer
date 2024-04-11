package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.ArrayList;
import java.util.List;

public class ScenarioFile extends PsiFileBase implements YAMLFile
{
    protected ScenarioFile(@NotNull FileViewProvider viewProvider)
    {
        super(viewProvider, ScenarioFileLanguage.INSTANCE);
    }

    @Override
    public List<YAMLDocument> getDocuments()
    {
        ASTNode[] cumentAST = this.getNode().getChildren(TokenSet.create(YAMLElementTypes.DOCUMENT));

        List<YAMLDocument> result = new ArrayList<>(cumentAST.length);
        for (ASTNode node : cumentAST)
            result.add((YAMLDocument) node.getPsi());

        return result;
    }

    @Override
    public @NotNull FileType getFileType()
    {
        return ScenarioFileType.INSTANCE;
    }
}
