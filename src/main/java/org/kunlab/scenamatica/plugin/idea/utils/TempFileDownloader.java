package org.kunlab.scenamatica.plugin.idea.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.openapi.vfs.impl.http.RemoteFileInfo;
import com.intellij.openapi.vfs.impl.http.RemoteFileState;

import java.io.InputStream;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TempFileDownloader
{
    public static final Logger LOGGER = Logger.getLogger(TempFileDownloader.class.getName());
    private static final Gson GSON = new Gson();

    public static VirtualFile download(String url, Consumer<? super VirtualFile> callback)
    {
        LOGGER.info("Downloading file: " + url);

        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf == null)
            throw new IllegalArgumentException("Cannot find file by url: " + url);

        RemoteFileInfo fi = ((HttpVirtualFile) vf).getFileInfo();
        if (fi == null)
            throw new IllegalStateException("Cannot get file info for url: " + url);
        if (fi.getLocalFile() != null)
        {
            LOGGER.info("File already downloaded: " + url);
            callback.accept(vf);
            return vf;
        }

        vf.refresh(false, true, () -> {
            LOGGER.info("File downloaded: " + url);
            if (fi.getState() != RemoteFileState.ERROR_OCCURRED)
                callback.accept(vf);
            else
                throw new IllegalStateException("Unable to download file from url: " + url + ", error: " + fi.getErrorMessage());
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
