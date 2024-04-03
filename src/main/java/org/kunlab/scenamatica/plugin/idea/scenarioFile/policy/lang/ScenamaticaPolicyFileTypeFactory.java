package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang;

import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageSchemaDescriptor;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

public class ScenamaticaPolicyFileTypeFactory implements FileTypeUsageSchemaDescriptor
{
    @Override
    public boolean describes(@NotNull Project project, @NotNull VirtualFile file)
    {
        return FileTypeRegistry.getInstance().isFileOfType(file, YAMLFileType.YML)
                && FileUtil.namesEqual(file.getName(), ScenamaticaPolicyFileType.FULL_NAME);
    }
}
