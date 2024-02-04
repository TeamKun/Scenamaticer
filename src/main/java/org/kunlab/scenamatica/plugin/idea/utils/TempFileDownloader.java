package org.kunlab.scenamatica.plugin.idea.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileInfo;
import com.intellij.openapi.vfs.impl.http.RemoteFileState;

import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TempFileDownloader
{
    public static final Logger LOGGER = Logger.getLogger(TempFileDownloader.class.getName());
    private static final Key<Object> KEY_DOWNLOAD_STARTED = Key.create("org.kunlab.scenamatica.plugin.idea.utils.TempFileDownloader.DownloadStarted");
    private static final Object VALUE_DOWNLOAD_STARTED = new Object();
    private static final Gson GSON = new Gson();
    private static final Map<VirtualFile, Consumer<? super VirtualFile>> DOWNLOAD_CALLBACKS = new WeakHashMap<>();

    static
    {
        // Create timer and poll changes
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            //noinspection InfiniteLoopStatement
            while (true)
            {
                try
                {
                    //noinspection BusyWait
                    Thread.sleep(100);
                    watchDog();
                }
                catch (InterruptedException ignored)
                {
                }
            }
        });
    }

    private static void watchDog()
    {
        for (VirtualFile vf : DOWNLOAD_CALLBACKS.keySet())
        {
            if (vf.getUserData(KEY_DOWNLOAD_STARTED) == null)
                continue;

            RemoteFileInfo fi = ((HttpVirtualFile) vf).getFileInfo();
            if (fi == null)
                continue;

            if (fi.getState() == RemoteFileState.DOWNLOADED)
            {
                vf.putUserData(KEY_DOWNLOAD_STARTED, null);
                Consumer<? super VirtualFile> callback = DOWNLOAD_CALLBACKS.remove(vf);
                if (callback != null)
                    callback.accept(vf);
            }
            else if (fi.getState() == RemoteFileState.ERROR_OCCURRED)
            {
                vf.putUserData(KEY_DOWNLOAD_STARTED, null);
                Consumer<? super VirtualFile> callback = DOWNLOAD_CALLBACKS.remove(vf);
                if (callback != null)
                    throw new IllegalStateException("Unable to download file from url: " + vf.getUrl() + ", error: " + fi.getErrorMessage());
            }
        }
    }

    public static VirtualFile download(String url, Consumer<? super VirtualFile> callback)
    {
        LOGGER.info("Downloading file: " + url);

        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf == null)
            throw new IllegalArgumentException("Cannot find file by url: " + url);
        else if (vf.getUserData(KEY_DOWNLOAD_STARTED) != null)
        {
            LOGGER.info("File already downloading: " + url);
            callback.accept(vf);
            return vf;
        }
        else
            vf.putUserData(KEY_DOWNLOAD_STARTED, VALUE_DOWNLOAD_STARTED);

        RemoteFileInfo fi = ((HttpVirtualFile) vf).getFileInfo();
        if (fi == null)
            throw new IllegalStateException("Cannot get file info for url: " + url);
        if (fi.getLocalFile() != null)
        {
            LOGGER.info("File already downloaded: " + url);
            callback.accept(vf);
            return vf;
        }

        DOWNLOAD_CALLBACKS.put(vf, callback);

        LOGGER.info("Start downloading file: " + url);
        vf.refresh(true, true, () -> {
            LOGGER.info("File downloaded: " + url);
            Consumer<? super VirtualFile> cb = DOWNLOAD_CALLBACKS.remove(vf);
            if (cb != null)
                cb.accept(vf);
        });

        return vf;
    }

    public static void downloadJson(String url, Consumer<? super JsonObject> callback)
    {
        download(url, (file) -> {
            try (InputStream is = file.getInputStream())
            {
                callback.accept(GSON.fromJson(new String(is.readAllBytes()), JsonObject.class));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    public static JsonObject downloadJsonSync(String path)
    {
        CyclicBarrier barrier = new CyclicBarrier(2);
        JsonObject[] result = new JsonObject[1];

        downloadJson(path, (json) -> {
            result[0] = json;
            try
            {
                barrier.await();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        try
        {
            barrier.await();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return result[0];
    }

    public static VirtualFile downloadSync(String url)
    {
        CyclicBarrier barrier = new CyclicBarrier(2);
        VirtualFile[] result = new VirtualFile[1];

        download(url, (file) -> {
            result[0] = file;
            try
            {
                barrier.await();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        try
        {
            barrier.await();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return result[0];
    }
}
