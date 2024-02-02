package org.kunlab.scenamatica.plugin.idea.refsBrowser;

import org.jetbrains.annotations.Nullable;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.utils.StringUtils;
import org.kunlab.scenamatica.plugin.idea.utils.URLUtils;

public class WebReference
{
    public static final String BASE_URL = "https://scenamatica.kunlab.org/docs/use/scenario/types/";

    public static String typeToWebReference(String type)
    {
        if (type == null || type.equals("prime"))
            return processAnchor(BASE_URL, "scenario-file");
        else if (SchemaProviderService.getProvider().hasDefinition(type))
        {
            String definitionsGroup = SchemaProviderService.getProvider().getMeta().getDefinitionGroup(type);
            if (definitionsGroup == null)
                return null;
            return switch (definitionsGroup)
            {
                case "scenamatica" -> processAnchor(BASE_URL, type);  // Scenamatica グループは root に配置される。
                case "entity" -> processAnchor(URLUtils.concat(BASE_URL, "entities"), type);
                default -> processAnchor(URLUtils.concat(BASE_URL, StringUtils.toKebabCase(definitionsGroup)), type);
            };
        }

        return processAnchor(BASE_URL + type, null);
    }

    private static String processAnchor(String url, @Nullable String anchor)
    {
        return anchor == null ? url: url + "#" + StringUtils.toKebabCase(anchor);
    }
}
