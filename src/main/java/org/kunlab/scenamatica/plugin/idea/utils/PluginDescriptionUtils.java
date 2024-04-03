package org.kunlab.scenamatica.plugin.idea.utils;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.util.List;

public class PluginDescriptionUtils
{
    private static final String FILE_PATTERN = "^(plugin|bukkit)$";

    public static VirtualFile findPluginDescription(Module module)
    {
        ModuleRootManager rootMgr = ModuleRootManager.getInstance(module);
        List<VirtualFile> resourceRoots = rootMgr.getSourceRoots(JavaResourceRootType.RESOURCE);

        for (VirtualFile resourceRoot : resourceRoots)
        {
            VirtualFile pluginYml = findPluginDescription(resourceRoot);
            if (pluginYml != null)
                return pluginYml;
        }

        return null;
    }

    public static VirtualFile findPluginDescription(VirtualFile root)
    {
        if (root.isDirectory())
            return null;

        for (VirtualFile child : root.getChildren())
        {
            if (child.isDirectory())
                continue;

            if (child.getNameWithoutExtension().matches(FILE_PATTERN))
                return child;
        }

        return null;
    }
}
