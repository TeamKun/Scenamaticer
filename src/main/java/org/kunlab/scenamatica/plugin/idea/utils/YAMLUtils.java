package org.kunlab.scenamatica.plugin.idea.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.yaml.YAMLElementType;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLQuotedText;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;

import java.util.ArrayList;
import java.util.List;

public class YAMLUtils
{
    public static boolean isYAMLRelated(PsiElement psiElement)
    {
        return psiElement instanceof YAMLScalar || psiElement instanceof YAMLKeyValue;
    }

    public static boolean isKey(PsiElement psiElement)
    {
        return psiElement instanceof LeafPsiElement
                && ((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.SCALAR_KEY;
    }

    public static boolean isKeyValue(PsiElement psiElement)
    {
        return psiElement instanceof YAMLKeyValue;
    }

    public static boolean isValue(PsiElement psiElement)
    {
        if (psiElement == null)
            return false;
        else if (psiElement instanceof YAMLValue)
            return true;

        IElementType elementType;
        if (psiElement instanceof LeafPsiElement)
            elementType = ((LeafPsiElement) psiElement).getElementType();
        else
            elementType = psiElement.getNode().getElementType();
        if (!(elementType instanceof YAMLElementType type))
            return false;
        return type == YAMLTokenTypes.SCALAR_DSTRING
                || type == YAMLTokenTypes.SCALAR_STRING
                || type == YAMLTokenTypes.SCALAR_TEXT
                || type == YAMLElementTypes.SCALAR_QUOTED_STRING
                || type == YAMLElementTypes.SCALAR_TEXT_VALUE
                || type == YAMLElementTypes.SCALAR_PLAIN_VALUE
                || type == YAMLElementTypes.SCALAR_LIST_VALUE;
    }

    private static PsiElement getKey(PsiElement psiElement)
    {
        if (isKey(psiElement))
            return psiElement;
        else if (psiElement instanceof YAMLKeyValue)
            return psiElement.getFirstChild();
        else if (isValue(psiElement))
            return psiElement.getParent().getFirstChild();
        else
            throw new IllegalArgumentException("Cannot get key from " + psiElement);
    }

    public static PsiElement getValue(PsiElement psiElement)
    {
        if (isKey(psiElement))
            return psiElement.getParent().getLastChild();
        else if (psiElement instanceof YAMLKeyValue)
            return psiElement.getLastChild();
        else if (isValue(psiElement))
            return psiElement;
        else
            throw new IllegalArgumentException("Cannot get value from " + psiElement);
    }

    public static String getKeyText(PsiElement psiElement)
    {
        if (psiElement instanceof YAMLBlockMappingImpl)
        {
            String text = psiElement.getText();
            if (text.endsWith(":"))
                return text.substring(0, text.length() - 1);
            else
                return text;
        }
        else if (psiElement instanceof YAMLSequenceItem)
        {
            PsiElement[] children = psiElement.getParent().getChildren();
            for (int i = 0; i < children.length; i++)
                if (children[i] == psiElement)
                    return "\0" + i + "\0";
        }
        return getKey(psiElement).getText();
    }

    public static String getValueTextByKey(PsiElement psiElement)
    {
        return getValueText(getValue(psiElement));
    }

    public static String getValueText(PsiElement value)
    {
        String text = value.getText();
        if (value instanceof YAMLQuotedText)
            return text.substring(1, text.length() - 1);
        else
            return text;
    }

    public static String[] getAbsoluteKeys(PsiElement psiElement)
    {
        List<String> keys = new ArrayList<>();
        PsiElement current = psiElement;

        while (current != null)
        {
            if (isKeyValue(current) || isSequenceItem(current))
                keys.add(0, getKeyText(current));
            current = current.getParent();
        }


        return keys.toArray(new String[0]);
    }

    private static boolean isSequenceItem(PsiElement current)
    {
        return current instanceof YAMLSequenceItem;
    }

    public static String getAbsoluteKeyText(PsiElement psiElement)
    {
        return String.join(".", getAbsoluteKeys(psiElement));
    }

    public static boolean isInKey(PsiElement element, String key)
    {
        String[] keys = getAbsoluteKeys(element);
        String[] keyParts = key.split("\\.");
        if (keys.length < keyParts.length)
            return false;

        for (int i = 0; i < keyParts.length; i++)
            if (!keys[i].equals(keyParts[i]))
                return false;

        return true;
    }

    public static PsiElement getValue(PsiFile file, String key)
    {
        if (!isValidYAMLFile(file))
            throw new IllegalArgumentException("Cannot get value of key " + key + " in file " + file);

        YAMLKeyValue kv = YAMLUtil.getQualifiedKeyInFile((YAMLFile) file, key.split("\\."));
        if (kv == null)
            throw new IllegalArgumentException("Cannot find key " + key + " in file " + file.getName());

        return kv.getValue();
    }

    public static String getValueText(PsiFile file, String key)
    {
        return getValueTextByKey(getValue(file, key));
    }

    public static boolean hasValidKey(PsiFile file, String key)
    {
        YAMLKeyValue kv = YAMLUtil.getQualifiedKeyInFile((YAMLFile) file, key.split("\\."));
        return kv != null && isValue(kv.getValue());
    }

    private static boolean isValidYAMLFile(PsiFile file)
    {
        return file != null && file.isValid() && file.getFileType() == YAMLFileType.YML;
    }

    public static PsiFile toPSIFile(Project proj, VirtualFile file)
    {
        if (file == null || file.isDirectory() || !file.isValid())
            return null;

        if (!ApplicationManager.getApplication().isReadAccessAllowed())
            return null;

        return PsiUtilCore.getPsiFile(proj, file);
    }

    public static boolean isTopLevelKey(PsiElement element)
    {
        return element.getParent().getParent().getParent() instanceof YAMLDocument
                || element.getParent().getParent() instanceof YAMLDocument;
    }
}
