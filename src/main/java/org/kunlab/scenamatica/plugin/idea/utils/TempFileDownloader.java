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
import kotlin.Pair;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TempFileDownloader
{
    public static final Logger LOGGER = Logger.getLogger(TempFileDownloader.class.getName());
    private static final Key<Object> KEY_DOWNLOAD_STARTED = Key.create("org.kunlab.scenamatica.plugin.idea.utils.TempFileDownloader.DownloadStarted");
    private static final Object VALUE_DOWNLOAD_STARTED = new Object();
    private static final Gson GSON = new Gson();
    private static final Queue<Pair<VirtualFile, Consumer<? super VirtualFile>>> DOWNLOADING_QUEUE = new ConcurrentLinkedQueue<>();

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

        Iterator<Pair<VirtualFile, Consumer<? super VirtualFile>>> it = DOWNLOADING_QUEUE.iterator();
        while (it.hasNext())
        {
            Pair<VirtualFile, Consumer<? super VirtualFile>> entry = it.next();
            VirtualFile vf = entry.getFirst();
            Consumer<? super VirtualFile> callback = entry.getSecond();
            if (vf.getUserData(KEY_DOWNLOAD_STARTED) == null)
            {
                it.remove();
                continue;
            }

            RemoteFileInfo fi = ((HttpVirtualFile) vf).getFileInfo();
            if (fi == null)
                continue;

            if (fi.getState() == RemoteFileState.DOWNLOADED)
            {
                it.remove();
                if (callback != null)
                    callback.accept(vf);
            }
            else if (fi.getState() == RemoteFileState.ERROR_OCCURRED)
            {
                it.remove();
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
            for (Pair<VirtualFile, Consumer<? super VirtualFile>> entry : DOWNLOADING_QUEUE)
            {
                if (entry.getFirst().getUrl().equals(url))
                {
                    DOWNLOADING_QUEUE.add(new Pair<>(vf, callback));
                    return vf;
                }
            }
        }
        else
            vf.putUserData(KEY_DOWNLOAD_STARTED, VALUE_DOWNLOAD_STARTED);

        RemoteFileInfo fi = ((HttpVirtualFile) vf).getFileInfo();
        if (fi == null)
            throw new IllegalStateException("Cannot get file info for url: " + url);
        if (fi.getLocalFile() != null)
        {
            LOGGER.info("File already downloaded: " + url);
            if (callback != null)
                callback.accept(vf);


            return vf;
        }

        DOWNLOADING_QUEUE.add(new Pair<>(vf, callback));

        LOGGER.info("Start downloading file: " + url);
        vf.refresh(true, true, () -> {
            LOGGER.info("File downloaded: " + url);
            if (vf.getUserData(KEY_DOWNLOAD_STARTED) == null)
                return;
            vf.putUserData(KEY_DOWNLOAD_STARTED, null);
            if (callback != null)
                callback.accept(vf);
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
        Object lock = new Object();
        JsonObject[] result = new JsonObject[1];


        downloadJson(path, (json) -> {
            result[0] = json;
            synchronized (lock)
            {
                lock.notifyAll();
            }
        });

        synchronized (lock)
        {
            try
            {
                if (result[0] == null)
                    lock.wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        return result[0];
    }

    public static VirtualFile downloadSync(String url)
    {
        Object lock = new Object();
        VirtualFile[] result = new VirtualFile[1];

        download(url, (file) -> {
            result[0] = file;
            synchronized (lock)
            {
                lock.notifyAll();
            }
        });

        synchronized (lock)
        {
            try
            {
                if (result[0] == null)
                    lock.wait();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        return result[0];
    }
}
