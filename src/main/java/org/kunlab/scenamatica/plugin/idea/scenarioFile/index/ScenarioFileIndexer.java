package org.kunlab.scenamatica.plugin.idea.scenarioFile.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.DefaultFileTypeSpecificWithProjectInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.IndexedFile;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.ScenarioFiles;
import org.kunlab.scenamatica.plugin.idea.scenarioFile.schema.SchemaProviderService;
import org.kunlab.scenamatica.plugin.idea.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ScenarioFileIndexer extends FileBasedIndexExtension<String, ScenarioFileIndex>
{

    public static final ID<String, ScenarioFileIndex> NAME = ID.create("org.kunlab.scenamatica.plugin.idea.scenarioFile.index.ScenarioFileIndex");

    @Override
    public @NotNull ID<String, ScenarioFileIndex> getName()
    {
        return NAME;
    }

    @Override
    public @NotNull DataIndexer<String, ScenarioFileIndex, FileContent> getIndexer()
    {
        return fileContent ->
        {
            PsiFile file = fileContent.getPsiFile();

            String name = YAMLUtils.getValueText(file, ScenarioFiles.KEY_NAME);
            assert name != null;
            String description;
            if (YAMLUtils.hasValidKey(file, ScenarioFiles.KEY_DESCRIPTION))
                description = YAMLUtils.getValueText(file, ScenarioFiles.KEY_DESCRIPTION);
            else
                description = "";

            SchemaProviderService.getResolver().createCacheAll(file);

            return Map.of(
                    name,
                    new ScenarioFileIndex(
                            name, description,
                            file.getVirtualFile().getPath()
                    )
            );
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor()
    {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<ScenarioFileIndex> getValueExternalizer()
    {
        return ScenarioFileIndex.EXTERNALIZER;
    }

    @Override
    public int getVersion()
    {
        return 1;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter()
    {
        //noinspection UnstableApiUsage
        return new DefaultFileTypeSpecificWithProjectInputFilter(YAMLFileType.YML)
        {
            @Override
            public boolean acceptInput(@NotNull IndexedFile file)
            {
                PsiFile psiFile = YAMLUtils.toPSIFile(file.getProject(), file.getFile());
                return ScenarioFiles.isScenarioFile(psiFile) && YAMLUtils.hasValidKey(psiFile, ScenarioFiles.KEY_NAME);
            }
        };
    }

    @Override
    public boolean dependsOnFileContent()
    {
        return true;
    }

    public static Collection<String> getAllScenarioKeys(Project proj)
    {
        return FileBasedIndex.getInstance().getAllKeys(NAME, proj);
    }

    public static Collection<ScenarioFileIndex> getAllScenarios(Project proj)
    {
        List<ScenarioFileIndex> scenarios = new ArrayList<>();
        FileBasedIndex.getInstance().processAllKeys(NAME, key ->
        {
            scenarios.addAll(FileBasedIndex.getInstance().getValues(NAME, key, GlobalSearchScope.projectScope(proj)));
            return true;
        }, proj);

        return scenarios;
    }

    public static boolean isDuplicated(Project proj, String name)
    {
        return FileBasedIndex.getInstance().getValues(NAME, name, GlobalSearchScope.projectScope(proj)).size() > 1;
    }

    public static boolean hasIndexFor(Project proj, String name)
    {
        return !FileBasedIndex.getInstance().getValues(NAME, name, GlobalSearchScope.projectScope(proj)).isEmpty();
    }

    public static List<ScenarioFileIndex> getIndicesFor(Project proj, String name)
    {
        return new ArrayList<>(FileBasedIndex.getInstance().getValues(NAME, name, GlobalSearchScope.projectScope(proj)));
    }
}
