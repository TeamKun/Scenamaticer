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

    public static boolean isValue(PsiElement psiElement)
    {
        if (psiElement.getParent() instanceof YAMLKeyValue)
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

    public static PsiElement getKey(PsiElement psiElement)
    {
        if (isKey(psiElement))
            return psiElement;
        else
            return psiElement.getParent().getParent().getFirstChild();
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
            return null;
    }

    public static String getKeyText(PsiElement psiElement)
    {
        return getKey(psiElement).getText();
    }

    public static String getValueText(PsiElement psiElement)
    {
        PsiElement value = getValue(psiElement);
        if (value == null)
            return null;

        return getValueText(psiElement, value);
    }

    public static String getValueText(PsiElement key, PsiElement value)
    {
        String text = value.getText();
        if (key instanceof YAMLQuotedText || value.getParent().getParent().getLastChild() instanceof YAMLQuotedText)
            return text.substring(1, text.length() - 1);
        else
            return text;
    }

    public static String[] getAbsoluteKeys(PsiElement psiElement)
    {
        List<String> keys = new ArrayList<>();
        PsiElement current = psiElement;
        keys.add(getKeyText(current));

        while (current != null)
        {
            if (isKey(current))
                keys.add(0, getKeyText(current));
            current = current.getParent();
        }

        return keys.toArray(new String[0]);
    }

    public static String getAbsoluteKeyText(PsiElement psiElement)
    {
        return String.join(".", getAbsoluteKeys(psiElement));
    }

    public static String isInKey(PsiElement element, String key)
    {
        String[] keys = getAbsoluteKeys(element);
        for (String s : keys)
        {
            if (s.equals(key) || "*".equals(s))
                return s;
        }
        return null;
    }

    public static PsiElement getValue(PsiFile file, String key)
    {
        if (file == null || !file.isValid() || file.getFileType() != YAMLFileType.YML)
            return null;

        YAMLKeyValue kv = YAMLUtil.getQualifiedKeyInFile((YAMLFile) file, key.split("\\."));
        if (kv == null)
            return null;

        return kv.getValue();
    }

    public static String getValueText(PsiFile file, String key)
    {
        PsiElement element = getValue(file, key);
        if (element == null)
            return null;
        else
            return getValueText(element);
    }

    public static boolean hasKey(PsiFile file, String key)
    {
        return getValue(file, key) != null;
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
        return element.getParent().getParent().getParent() instanceof YAMLDocument;
    }
}
