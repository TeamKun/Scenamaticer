package org.kunlab.scenamatica.plugin.idea.scenarioFile.schema;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.http.FileDownloadingAdapter;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileInfo;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.kunlab.scenamatica.plugin.idea.utils.URLUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class SchemaProvider
{
    public static final String PATH_FILE_META = "meta.json";

    private final Map<String, VirtualFile> actionsCache;
    private final Map<String, VirtualFile> definitionCache;

    private String contentServerURL;

    @Getter
    private VirtualFile metaFile;
    @Getter
    private SchemaMeta meta;

    public SchemaProvider(String contentServerURL)
    {
        this.contentServerURL = contentServerURL;
        this.actionsCache = new HashMap<>();
        this.definitionCache = new HashMap<>();

        this.setContentServerURL(contentServerURL);
    }

    public void setContentServerURL(String contentServerURL)
    {
        this.contentServerURL = contentServerURL;
        this.updateFiles(true);
    }

    public VirtualFile getActionFile(String action)
    {
        checkMetaLoaded();

        if (this.actionsCache.containsKey(action))
            return this.actionsCache.get(action);

        ensureActionExistence(action);

        String group = this.meta.getActionGroup(action);
        String path = buildActionFilePath(action, group);
        VirtualFile file = getFileByUrl(path);

        this.actionsCache.put(action, file);
        return file;
    }

    public VirtualFile getDefinitionFile(String definition)
    {
        checkMetaLoaded();

        if (this.definitionCache.containsKey(definition))
            return this.definitionCache.get(definition);

        validateDefinitionExistence(definition);

        String path = buildDefinitionFilePath(definition);
        VirtualFile file = getFileByUrl(path);

        this.definitionCache.put(definition, file);
        return file;
    }

    private void validateDefinitionExistence(String definition)
    {
        if (!this.meta.isDefinitionExists(definition))
            throw new IllegalStateException("Definition '" + definition + "' does not exist");
    }

    private String buildDefinitionFilePath(String definition)
    {
        return URLUtils.concat(this.contentServerURL, this.meta.getDefinitionsDir(), definition + ".json");
    }

    private void checkMetaLoaded()
    {
        if (this.meta == null)
            throw new IllegalStateException("Meta file is not loaded yet");
    }

    private void ensureActionExistence(String action)
    {
        if (!this.meta.isActionExists(action))
            throw new IllegalStateException("Action '" + action + "' does not exist");
    }

    private String buildActionFilePath(String action, String group)
    {
        return URLUtils.concat(this.contentServerURL, this.meta.getActionsDir(), group, action + ".json");
    }

    private void updateFiles(boolean changed)
    {
        if (this.metaFile == null || changed)
            this.metaFile = VirtualFileManager.getInstance().findFileByUrl(URLUtils.concat(this.contentServerURL, PATH_FILE_META));
        if (this.metaFile == null)
            throw new IllegalStateException("Failed to download meta file from the Scenamatica content server: " + this.contentServerURL);

        RemoteFileInfo info = ((HttpVirtualFile) this.metaFile).getFileInfo();
        if (info == null)
            throw new IllegalStateException("Failed to download meta file from the Scenamatica content server: " + this.contentServerURL);

        info.addDownloadingListener(new FileDownloadingAdapter()
        {
            @Override
            public void fileDownloaded(@NotNull VirtualFile localFile)
            {
                SchemaProvider.this.meta = SchemaMeta.fromJSON(SchemaProvider.this.metaFile);
            }
        });
        this.metaFile.refresh(true, false);
    }

    private static VirtualFile getFileByUrl(String url)
    {
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file == null)
            throw new IllegalStateException("Failed to download " + url + "  from the Scenamatica content server: " + url);

        CountDownLatch latch = new CountDownLatch(1);
        file.refresh(true, false, latch::countDown);

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }

        return file;
    }
}
