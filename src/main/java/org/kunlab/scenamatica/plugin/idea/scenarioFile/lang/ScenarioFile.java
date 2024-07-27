package org.kunlab.scenamatica.plugin.idea.scenarioFile.lang;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.List;

public class ScenarioFile extends PsiFileBase implements YAMLFile
{
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";

    private static final String KEY_MINECRAFT_VERSION = "minecraft.";
    private static final String KEY_MINECRAFT_SINCE = KEY_MINECRAFT_VERSION + "since";
    private static final String KEY_MINECRAFT_UNTIL = KEY_MINECRAFT_VERSION + "until";

    protected ScenarioFile(@NotNull FileViewProvider viewProvider)
    {
        super(viewProvider, ScenarioFileLanguage.INSTANCE);
    }

    @NotNull
    public MinecraftVersion getSince()
    {
        try
        {
            return MinecraftVersion.fromString(YAMLUtils.getUnquotedValueText(this, KEY_MINECRAFT_SINCE));
        }
        catch (IllegalArgumentException e)
        {
            return MinecraftVersion.ANY;
        }
    }

    @NotNull
    public MinecraftVersion getUntil()
    {
        try
        {
            return MinecraftVersion.fromString(YAMLUtils.getUnquotedValueText(this, KEY_MINECRAFT_UNTIL));
        }
        catch (IllegalArgumentException e)
        {
            return MinecraftVersion.ANY;
        }
    }

    @NotNull
    public String getScenarioName()
    {
        return YAMLUtils.getUnquotedValueText(this, KEY_NAME);
    }

    @Nullable
    public String getDescription()
    {
        if (YAMLUtils.hasValidKey(this, KEY_DESCRIPTION))
            return YAMLUtils.getUnquotedValueText(this, KEY_DESCRIPTION);
        else
            return null;
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
