package org.kunlab.scenamatica.plugin.idea.scenarioFile;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileContentsChangedAdapter;
import org.jetbrains.annotations.NotNull;

public class ScenarioFileListener extends VirtualFileContentsChangedAdapter
{
    @Override
    protected void onFileChange(@NotNull VirtualFile virtualFile)
    {

    }

    @Override
    protected void onBeforeFileChange(@NotNull VirtualFile virtualFile)
    {
    }
}
