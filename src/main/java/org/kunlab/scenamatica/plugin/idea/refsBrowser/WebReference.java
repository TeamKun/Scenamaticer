package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.utils.StringUtils;
import org.kunlab.scenamatica.plugin.idea.utils.URLUtils;

public class WebReference
{
    public static final String BASE_URL = "https://scenamatica.kunlab.org/docs/use";
    public static final String BASE_URL_TYPE = BASE_URL + "/scenario/types/";
    public static final String BASE_URL_ACTION = BASE_URL + "/scenario/actions/";

    public static String findElementReferenceURL(PsiElement element)
    {
        String typeName = resolveTypeName(element);
        if (typeName == null && isActionSpecifier(element))
            return actionToWebReference(resolveActionName(element));
        else
            return typeToWebReference(typeName);
    }

    public static boolean isActionSpecifier(PsiElement element)
    {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> isActionSpecifier$0(element));
    }

    private static boolean isActionSpecifier$0(PsiElement element)
    {
        YAMLValue yamlValue;
        if (element instanceof YAMLValue)
            yamlValue = (YAMLValue) element;
        else if (element instanceof LeafPsiElement)
        {
            PsiElement parent = element.getParent();
            if (parent instanceof YAMLValue)
                yamlValue = (YAMLValue) parent;
            else if (parent instanceof YAMLKeyValue)
                return element.textMatches("action");
            else
                return false;
        }
        else
            return false;

        YAMLPsiElement parentContainer = (YAMLPsiElement) yamlValue.getParent();
        if (parentContainer instanceof YAMLSequenceItem)
            parentContainer = (YAMLPsiElement) parentContainer.getParent();
        

        YAMLMapping blockMapping = (YAMLBlockMappingImpl) parentContainer.getParent();
        if (blockMapping == null)
            return false;

        YAMLKeyValue actionKV = blockMapping.getKeyValueByKey("action");
        if (actionKV == null)
            return false;

        if (actionKV.getValue() == null)
            return false;

        return actionKV.getValue().equals(yamlValue);
    }

    private static String resolveActionName(PsiElement element)
    {
        return SchemaProviderService.getResolver().getActionName(element);
    }

    private static String resolveTypeName(PsiElement element)
    {
        return SchemaProviderService.getResolver().getTypeName(element);
    }

    public static String typeToWebReference(String type)
    {
        if (type == null || type.equals("prime"))
            return processAnchor(BASE_URL_TYPE, "scenario-file");
        else if (SchemaProviderService.getProvider().hasDefinition(type))
        {
            String definitionsGroup = SchemaProviderService.getProvider().getMeta().getDefinitionGroupOf(type);
            if (definitionsGroup == null)
                return null;
            if (definitionsGroup.equals("scenamatica"))
                return processAnchor(BASE_URL_TYPE, type);  // Scenamatica グループは root に配置される。
            else if ("entity".equals(definitionsGroup) || definitionsGroup.startsWith("entities/"))
                return processAnchor(URLUtils.concat(BASE_URL_TYPE, "entities"), type);
            else
                return processAnchor(URLUtils.concat(BASE_URL_TYPE, StringUtils.toKebabCase(definitionsGroup)), type);
        }

        return processAnchor(BASE_URL_TYPE + type, null);
    }

    private static String actionToWebReference(String action)
    {
        String actionsGroup = SchemaProviderService.getProvider().getMeta().getActionGroupOf(action);
        if (actionsGroup == null)
            return null;

        String kebabAction = StringUtils.toKebabCase(action);
        String kebabGroup = StringUtils.toKebabCase(actionsGroup);
        String[] groupComponents = kebabGroup.split("/");

        if (kebabAction.startsWith(actionsGroup + "-"))
            kebabAction = kebabAction.substring(actionsGroup.length() + "-".length());
        else if (groupComponents.length > 1 && kebabAction.startsWith(groupComponents[1] + "-"))
            kebabAction = kebabAction.substring(groupComponents[1].length() + "-".length());

        return processAnchor(URLUtils.concat(BASE_URL_ACTION, kebabGroup), kebabAction);
    }

    private static String processAnchor(String url, @Nullable String anchor)
    {
        return anchor == null ? url: url + "#" + StringUtils.toKebabCase(anchor);
    }
}
