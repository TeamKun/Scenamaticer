package org.kunlab.scenamatica.plugin.idea.scenarioFile.policy;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndexer;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.policy.lang.ScenamaticaPolicyFileType;

import java.util.List;

@Service(Service.Level.PROJECT)
public final class ScenamaticaPolicyUpdater implements BulkFileListener, Disposable
{
    private final Project project;
    private final MessageBusConnection conn;

    public ScenamaticaPolicyUpdater(Project project)
    {
        this.project = project;
        this.conn = ApplicationManager.getApplication().getMessageBus().connect();
        this.conn.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    @Override
    public void dispose()
    {
        this.conn.disconnect();
    }

    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events)
    {
        if (!shouldRebuildIndex(events))
            return;

        FileBasedIndex.getInstance().requestRebuild(ScenarioFileIndexer.NAME, new Throwable());
    }

    public static @NotNull ScenamaticaPolicyUpdater getInstance(Project project)
    {
        return project.getService(ScenamaticaPolicyUpdater.class);
    }

    private static boolean shouldRebuildIndex(@NotNull List<? extends @NotNull VFileEvent> events)
    {
        for (VFileEvent event : events)
        {
            if (event.getFile() == null)
                continue;

            if (event.getFile().getFileType() == ScenamaticaPolicyFileType.INSTANCE)
                return true;
        }

        return false;
    }
}
