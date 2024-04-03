package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLValue;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;

import java.util.ArrayList;
import java.util.List;

public class ScenamaticaPolicyFile extends PsiFileBase implements YAMLFile, ScenamaticaPolicy
{
    private static final String KEY_MINECRAFT_VERSION = "minecraft";

    protected ScenamaticaPolicyFile(@NotNull FileViewProvider viewProvider)
    {
        super(viewProvider, ScenamaticaPolicyLanguage.INSTANCE);
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
        return ScenamaticaPolicyFileType.INSTANCE;
    }

    @Override
    @Nullable
    public MinecraftVersion getMinecraftVersion()
    {
        YAMLPsiElement value = this.getValue(KEY_MINECRAFT_VERSION);
        if (value == null)
            return null;

        return MinecraftVersion.fromString(value.getText());
    }

    @Nullable
    private YAMLPsiElement getValue(@NotNull String key)
    {
        YAMLValue topLevelValue = this.getDocuments().get(0).getTopLevelValue();
        if (topLevelValue == null)
            return null;

        assert topLevelValue instanceof YAMLMapping;
        YAMLMapping mapping = (YAMLMapping) topLevelValue;

        YAMLKeyValue keyValue = mapping.getKeyValueByKey(key);

        return keyValue != null ? keyValue.getValue(): null;
    }

    @Nullable
    public static ScenamaticaPolicy tryGetPolicyOf(@NotNull Project project, @NotNull VirtualFile scenarioFile)
    {
        VirtualFile policyFile = null;
        VirtualFile current = scenarioFile;
        while (current != null)
        {
            if (!current.isDirectory())
            {
                current = current.getParent();
                continue;
            }

            VirtualFile mayPolicyFile = current.findChild(ScenamaticaPolicyFileType.FULL_NAME);
            if (mayPolicyFile != null && mayPolicyFile.getFileType() == ScenamaticaPolicyFileType.INSTANCE)
            {
                policyFile = mayPolicyFile;
                break;
            }

            current = current.getParent();
        }

        if (policyFile == null)
            return null;

        PsiManager psiManager = PsiManager.getInstance(project);
        return (ScenamaticaPolicy) psiManager.findFile(policyFile);
    }
}
