package me.xiba.plugin.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.psi.impl.source.tree.java.PsiPackageStatementImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PsiClassUtil {
    /**
     * 获取类包名
     *
     * @param psiJavaFile
     * @return
     */
    public static String getPackageName(PsiJavaFile psiJavaFile) {
        PsiElement[] psiElements = psiJavaFile.getChildren();
        for (PsiElement element : psiElements) {
            if (element instanceof PsiPackageStatementImpl) {
                return ((PsiPackageStatementImpl) element).getPackageName();
            }
        }
        return "";
    }

    /**
     * 获取类中的变量及类型
     *
     * @param psiClass
     * @return
     */
    public static Map<String, String> getFields(PsiClassImpl psiClass) {
        Map<String, String> fieldMap = new HashMap<>();
        PsiField[] psiFields = psiClass.getAllFields();
        for (PsiField field : psiFields) {
            fieldMap.put(field.getName(), field.getType().getPresentableText());
        }
        return fieldMap;
    }

    /**
     * 获取类的构造函数
     *
     * @param psiClass
     * @return
     */
    public static List<String> getConstructors(PsiClassImpl psiClass) {
        List<String> constructors = new ArrayList<>();
        PsiMethod[] psiMethods = psiClass.getConstructors();
        for (PsiMethod method : psiMethods) {
            if (method.getBody() != null) {
                constructors.add(method.getText());
            }
        }
        return constructors;
    }

    /**
     * 获取类中的全部方法
     *
     * @param psiClass
     * @return
     */
    public static List<String> getMethods(PsiClassImpl psiClass) {
        List<String> methods = new ArrayList<>();
        PsiMethod[] psiMethods = psiClass.getMethods();
        for (PsiMethod method : psiMethods) {
            if (method.getBody() != null) {
                methods.add(method.getText());
            }
        }
        return methods;
    }

    /**
     * 根据方法名查找方法
     *
     * @param psiClass
     * @param methodName
     * @return
     */
    public static List<String> getMethodsByName(PsiClassImpl psiClass, String methodName) {
        List<String> methods = new ArrayList<>();
        PsiMethod[] psiMethods = psiClass.getMethods();
        for (PsiMethod method : psiMethods) {
            if (method.getBody() != null && method.getText().contains(methodName)) {
                methods.add(method.getText());
            }
        }
        return methods;
    }
}
