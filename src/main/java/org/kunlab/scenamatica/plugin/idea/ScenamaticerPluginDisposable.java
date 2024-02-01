package org.kunlab.scenamatica.plugin.idea;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.PROJECT)
public final class ScenamaticerPluginDisposable implements Disposable
{
    @Override
    public void dispose()
    {
        // 親子関係のためのダミー Dispose メソッド
    }

    public static @NotNull Disposable getInstance(Project project)
    {
        return project.getService(ScenamaticerPluginDisposable.class);
    }
}
