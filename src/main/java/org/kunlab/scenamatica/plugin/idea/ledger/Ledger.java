package org.kunlab.scenamatica.plugin.idea.ledger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kunlab.scenamatica.plugin.idea.ledger.models.ILedgerContent;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerAction;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerCategory;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerReference;
import org.kunlab.scenamatica.plugin.idea.ledger.models.LedgerType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Getter
public class Ledger
{
    public static final String OFFICIAL_LEDGER_NAME = "official";

    private static final Path PATH_ACTIONS = Paths.get("actions");
    private static final Path PATH_TYPES = Paths.get("types");
    private static final Path PATH_CATEGORIES = Paths.get("categories");

    private static final Logger LOG = Logger.getInstance(Ledger.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private final String ledgerName;
    private final Path ledgerPath;
    private final Path zipPath;
    private final Path cachePath;

    private final Map<LedgerReference, LedgerAction> actions;
    private final Map<LedgerReference, LedgerType> types;
    private final Map<LedgerReference, LedgerCategory> categories;
    private final Map<LedgerReference, JsonNode> ledgerData;

    private URL ledgerURL;

    public Ledger(String ledgerName, Path basePath, URL ledgerURL)
    {
        this.ledgerURL = ledgerURL;
        this.ledgerName = ledgerName;
        this.ledgerPath = basePath.resolve(this.ledgerName);
        this.zipPath = this.ledgerPath.resolve("ledger.zip");
        this.cachePath = this.ledgerPath.resolve("ledgers");

        this.actions = new HashMap<>();
        this.types = new HashMap<>();
        this.categories = new HashMap<>();
        this.ledgerData = new HashMap<>();
    }

    public boolean buildCache()
    {
        LOG.info("Building cache for ledger: " + this.ledgerName);
        if (!createDirectory(this.ledgerPath))
            return false;

        if (downloadLedger(this.ledgerURL, this.zipPath) == null)
        {
            LOG.error("Failed to build cache for ledger: " + this.ledgerName);
            return false;
        }

        LOG.info("Scanning ledger: " + this.ledgerName);
        // 中のファイルを解凍する
        if (unzipTo(this.zipPath, this.cachePath) == null)
        {
            LOG.error("Failed to build cache for ledger: " + this.ledgerName);
            return false;
        }

        readLedgerContents(this.cachePath.resolve(PATH_ACTIONS), this.actions, LedgerAction.class);
        readLedgerContents(this.cachePath.resolve(PATH_TYPES), this.types, LedgerType.class);
        readLedgerContents(this.cachePath.resolve(PATH_CATEGORIES), this.categories, LedgerCategory.class);

        return true;
    }

    private void deleteLedgerCacheFiles()
    {
        try (Stream<Path> paths = Files.walk(this.cachePath))
        {
            paths.map(Path::toFile).forEach(file -> {
                if (!file.delete())
                    LOG.error("Failed to delete file: " + file);
            });
        }
        catch (IOException e)
        {
            LOG.error("Failed to clean cache for ledger: " + this.ledgerName, e);
        }
    }

    public void cleanCache()
    {
        LOG.info("Cleaning cache for ledger: " + this.ledgerName);

        this.actions.clear();
        this.types.clear();
        this.ledgerData.clear();

        this.deleteLedgerCacheFiles();

    }

    public void setLedgerURL(URL ledgerURL)
    {
        this.cleanCache();
        this.ledgerURL = ledgerURL;
        this.buildCache();
    }

    public boolean isOfficial()
    {
        return OFFICIAL_LEDGER_NAME.equals(this.ledgerName);
    }

    private static <T extends ILedgerContent> void readLedgerContents(Path directory, Map<? super LedgerReference, ? super T> field, Class<? extends T> type)
    {
        try (Stream<Path> paths = Files.walk(directory))
        {
            paths.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(file -> {
                        try
                        {
                            T content = MAPPER.readValue(file, type);
                            field.put(content.getReference(), content);
                        }
                        catch (IOException e)
                        {
                            LOG.error("Failed to read ledger content: " + file, e);
                        }
                    });
        }
        catch (IOException e)
        {
            LOG.error("Failed to read ledger contents: " + directory, e);
        }
    }

    private static boolean createDirectory(Path path)
    {
        if (Files.exists(path))
            return true;

        try
        {
            Files.createDirectories(path);
            return true;
        }
        catch (IOException e)
        {
            LOG.error("Failed to create directory: " + path, e);
            return false;
        }
    }

    private static Path unzipTo(Path zipFile, Path destDir)
    {
        if (Files.exists(destDir))
        {
            LOG.info("Deleting existing directory: " + destDir);
            try (Stream<Path> paths = Files.walk(destDir))
            {
                paths.map(Path::toFile).forEach(file -> {
                    if (!file.delete())
                        LOG.error("Failed to delete file: " + file);
                });
            }
            catch (IOException e)
            {
                LOG.error("Failed to delete existing directory: " + destDir, e);
            }
        }

        LOG.info("Unzipping file: " + zipFile + " to: " + destDir);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile())))
        {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                Path entryPath = destDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(destDir)) // パストラバーサル回避
                {
                    LOG.error("Entry is outside of the target dir: " + entry.getName());
                    return null;
                }
                else if (entry.isDirectory())
                {
                    Files.createDirectories(entryPath);
                    continue;
                }

                Files.createDirectories(entryPath.getParent());
                try (OutputStream os = Files.newOutputStream(entryPath))
                {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1)
                        os.write(buffer, 0, bytesRead);
                }
            }
        }
        catch (IOException e)
        {
            LOG.error("Failed to unzip file: " + zipFile, e);
            return null;
        }

        return destDir;
    }

    private static boolean shouldUseCachedLedger(Path target, CloseableHttpResponse response)
    {
        if (!Files.exists(target))
            return false;

        ZonedDateTime lastModifiedAt;
        try
        {
            FileTime lastModifiedTime = Files.getLastModifiedTime(target);
            lastModifiedAt = ZonedDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault());
        }
        catch (IOException e)
        {
            LOG.error("Failed to get last modified time of file: " + target, e);
            return false;
        }

        Header cacheControlHeader = response.getFirstHeader("Cache-Control");
        Header lastModifiedHeader = response.getFirstHeader("Last-Modified");
        if (cacheControlHeader != null && cacheControlHeader.getValue().contains("no-cache"))
            return false;
        else if (lastModifiedHeader != null)
        {
            String lastModifiedStr = lastModifiedHeader.getValue();
            ZonedDateTime lastModified = ZonedDateTime.parse(lastModifiedStr, DateTimeFormatter.RFC_1123_DATE_TIME);
            return lastModified.isBefore(lastModifiedAt);
        }

        return false;
    }

    private static Path downloadLedger(URL ledgerURL, Path downloadDist)
    {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet(ledgerURL.toURI())))
        {
            if (response.getStatusLine().getStatusCode() != 200)
            {
                LOG.error("Failed to download ledger: " + ledgerURL + ", status code: " + response.getStatusLine().getStatusCode());
                return null;
            }
            else if (shouldUseCachedLedger(downloadDist, response))
            {
                LOG.info("Using cached ledger: " + ledgerURL);
                return downloadDist;
            }

            HttpEntity entity = response.getEntity();
            if (entity == null)
            {
                LOG.error("Failed to download ledger: " + ledgerURL);
                return null;
            }


            File saveFile = downloadDist.toFile();
            try (InputStream is = entity.getContent();
                 OutputStream os = new FileOutputStream(saveFile))
            {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1)
                    os.write(buffer, 0, bytesRead);
            }

            return saveFile.toPath();
        }
        catch (Exception e)
        {
            LOG.error("Failed to download ledger: " + ledgerURL, e);
            return null;
        }
    }
}
