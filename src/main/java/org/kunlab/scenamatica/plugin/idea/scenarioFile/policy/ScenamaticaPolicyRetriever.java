package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicyFile;
import org.kunlab.scenamatica.plugin.idea.utils.PluginDescriptionUtils;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

public class ScenamaticaPolicyRetriever
{
    @NotNull
    public static ScenamaticaPolicy retrieveOrGuessPolicy(ScenamaticaPolicyVisitor visitor, Project project, VirtualFile scenarioFile)
    {
        ScenamaticaPolicy explicitPolicy = ScenamaticaPolicyFile.tryGetPolicyOf(project, scenarioFile);
        if (explicitPolicy != null && visitor.visit(explicitPolicy))
            return explicitPolicy;
        // すべてのモジュールを処理する
        for (Module module : ModuleManager.getInstance(project).getModules())
        {
            VirtualFile[] roots = ModuleRootManager.getInstance(module).getSourceRoots();
            for (VirtualFile root : roots)
            {
                if (isInDescendants(root, scenarioFile))
                {
                    ScenamaticaPolicy policy = guessByPluginYAML(project, module);
                    if (policy == null)
                        continue;
                    if (visitor.visit(policy))
                        return policy;
                    break;
                }
            }
        }

        // プロジェクト全体を処理する
        ScenamaticaPolicy policy = DefaultScenamaticaPolicy.INSTANCE;
        visitor.visit(policy);
        return policy;
    }

    private static boolean isInDescendants(@NotNull VirtualFile parent, @NotNull VirtualFile child)
    {
        VirtualFile current = child;
        while (current != null)
        {
            if (current.equals(parent))
                return true;

            current = current.getParent();
        }

        return false;
    }

    private static ScenamaticaPolicy guessByPluginYAML(@NotNull Project project, @NotNull Module currentModule)
    {
        VirtualFile pluginYaml = PluginDescriptionUtils.findPluginDescription(currentModule);
        if (pluginYaml == null)
        {
            for (Module module : ModuleManager.getInstance(project).getModules())
            {
                pluginYaml = PluginDescriptionUtils.findPluginDescription(module);
                if (pluginYaml != null)
                    break;
            }

            if (pluginYaml == null)
                return null;
        }

        return getByPluginYAML(project, pluginYaml);
    }

    private static ScenamaticaPolicy getByPluginYAML(@NotNull Project project, @NotNull VirtualFile pluginYamlFile)
    {

        PsiFile noneTypePluginYaml = PsiManager.getInstance(project).findFile(pluginYamlFile);
        assert noneTypePluginYaml instanceof YAMLFile;
        YAMLFile yamlFile = (YAMLFile) noneTypePluginYaml;

        YAMLDocument firstDocument = yamlFile.getDocuments().get(0);
        YAMLMapping mapping = (YAMLMapping) firstDocument.getTopLevelValue();
        assert mapping != null;

        MinecraftVersion apiVersion = null;
        YAMLKeyValue apiVersionKV;
        if ((apiVersionKV = mapping.getKeyValueByKey("api-version")) != null)
        {
            String apiVersionStr = YAMLUtils.getValueText(apiVersionKV.getValue());
            if (apiVersionStr.matches("^\\d+\\.\\d+$"))
                apiVersion = MinecraftVersion.fromString(apiVersionStr + ".x");
        }

        return new GuessedScenamaticaPolicy(
                apiVersion
        );
    }

    public interface ScenamaticaPolicyVisitor
    {
        boolean visit(@NotNull ScenamaticaPolicy policy);
    }
}
