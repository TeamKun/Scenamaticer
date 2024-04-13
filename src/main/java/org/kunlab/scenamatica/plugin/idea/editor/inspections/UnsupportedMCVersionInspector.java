package org.kunlab.scenamatica.plugin.idea.editor.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.kunlab.scenamatica.plugin.idea.ScenamaticerBundle;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.lang.ScenarioFile;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.MinecraftVersion;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.ScenamaticaPolicyRetriever;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicy;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaAction;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaResolver;

public class UnsupportedMCVersionInspector extends AbstractScenamaticaActionElementInspection
{
    public static final Key<ScenamaticaPolicy> KEY_POLICY = new Key<>("scenamatica:policy");
    public static final Key<MinecraftVersion> KEY_VERSION_SINCE = new Key<>("scenamatica:version:since");
    public static final Key<MinecraftVersion> KEY_VERSION_UNTIL = new Key<>("scenamatica:version:until");
    public static final String ID = "UnsupportedMCVersion";

    public UnsupportedMCVersionInspector()
    {
        super(ID, "Unsupported Minecraft Version", HighlightDisplayLevel.WEAK_WARNING);
    }

    @Override
    protected boolean checkAction(@NotNull ProblemsHolder holder, SchemaResolver.@NotNull ScenarioAction action, @NotNull SchemaAction actionDefinition, @NotNull YAMLKeyValue actionKV)
    {
        checkActionVersionCompatibility(holder, action, actionDefinition);

        return true;
    }

    @Override
    protected @Nullable Key<?>[] getTempFileKeys()
    {
        return new Key<?>[]{KEY_VERSION_SINCE, KEY_VERSION_UNTIL, KEY_POLICY};
    }

    private static void checkActionVersionCompatibility(@NotNull ProblemsHolder holder, @NotNull SchemaResolver.ScenarioAction action, @NotNull SchemaAction actionDef)
    {
        Pair<MinecraftVersion, MinecraftVersion> fileSpecifiedVersionRange = retrieveVersionRange((ScenarioFile) action.getActionName().getContainingFile());
        MinecraftVersion fileSince = fileSpecifiedVersionRange.getFirst();
        MinecraftVersion fileUntil = fileSpecifiedVersionRange.getSecond();

        ScenamaticaPolicy policy = retrievePolicy((ScenarioFile) action.getActionName().getContainingFile());
        MinecraftVersion serverVersion = policy.getMinecraftVersion();
        if (serverVersion == null)
            serverVersion = MinecraftVersion.ANY;

        MinecraftVersion actionSince = actionDef.since();
        MinecraftVersion actionUntil = actionDef.until();

        if ((isUnspecified(actionSince) && isUnspecified(actionUntil))
                || (isUnspecified(fileSince) && isUnspecified(fileUntil) && isUnspecified(serverVersion)))
            return;

        boolean predicateByFileSince = isSpecified(fileSince) && !fileSince.isInRange(actionSince, actionUntil);
        boolean predicateByFileUntil = isSpecified(fileUntil) && !fileUntil.isInRange(actionSince, actionUntil);
        boolean predicateByServerVersion = (isUnspecified(fileSince) && isUnspecified(fileUntil)) // file を優先
                && isSpecified(serverVersion) && !serverVersion.isInRange(actionSince, actionUntil);

        MinecraftVersion exampleOfUnavailableVersion = null;
        if (predicateByFileSince)
            exampleOfUnavailableVersion = fileSince;
        else if (predicateByFileUntil)
            exampleOfUnavailableVersion = fileUntil;
        else if (predicateByServerVersion)
            exampleOfUnavailableVersion = serverVersion;

        if (predicateByFileSince || predicateByFileUntil || predicateByServerVersion)
            registerVersionProblem(holder, action.getActionName(), actionDef, exampleOfUnavailableVersion);
    }

    private static void registerVersionProblem(@NotNull ProblemsHolder holder, @NotNull PsiElement element,
                                               @NotNull SchemaAction actionDef, @NotNull MinecraftVersion exampleOfUnavailableVersion)
    {
        MinecraftVersion actionSince = actionDef.since();
        MinecraftVersion actionUntil = actionDef.until();

        String description;
        if (actionSince == null)
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.until",
                    actionUntil,
                    exampleOfUnavailableVersion
            );
        else if (actionUntil == null)
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.since",
                    actionSince,
                    exampleOfUnavailableVersion
            );
        else
            description = ScenamaticerBundle.of(
                    "editor.inspections.unsupportedActionUsage.version.description.ranged",
                    actionSince,
                    actionUntil,
                    exampleOfUnavailableVersion
            );

        holder.registerProblem(
                element,
                description
        );
    }

    private static boolean isSpecified(@Nullable MinecraftVersion version)
    {
        return !(version == null || version == MinecraftVersion.ANY);
    }

    private static boolean isUnspecified(@Nullable MinecraftVersion version)
    {
        return !isSpecified(version);
    }

    private static Pair<MinecraftVersion, MinecraftVersion> retrieveVersionRange(@NotNull ScenarioFile file)
    {
        MinecraftVersion since = file.getUserData(KEY_VERSION_SINCE);
        MinecraftVersion until = file.getUserData(KEY_VERSION_UNTIL);
        if (since == null || until == null)
        {
            since = file.getSince();
            until = file.getUntil();

            file.putUserData(KEY_VERSION_SINCE, since);
        }
        return Pair.create(since, until);
    }

    private static ScenamaticaPolicy retrievePolicy(@NotNull ScenarioFile file)
    {
        ScenamaticaPolicy policy = file.getUserData(KEY_POLICY);
        if (policy == null)
        {
            policy = ScenamaticaPolicyRetriever.retrieveOrGuessPolicy(
                    proposal -> proposal.getMinecraftVersion() != null,
                    file.getProject(),
                    file.getVirtualFile()
            );
            file.putUserData(KEY_POLICY, policy);
        }

        return policy;
    }
}
