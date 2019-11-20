package me.xiba.plugin.utils;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ImportPath;

public class KtPsiCreator {
    private Project project;
    private KtPsiFactory mKtPsiFactory;

    public KtPsiCreator(Project project, KtPsiFactory ktPsiFactory) {
        this.project = project;
        mKtPsiFactory = ktPsiFactory;
    }

    /**
     * 创建一个变量
     * @param text
     * @param context
     * @return
     */
    public PsiElement createField(String text, PsiElement context, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
//                KtProperty psiField = mKtPsiFactory.createProperty(text);
                KtBlockCodeFragment ktBlockCodeFragment = mKtPsiFactory.createBlockCodeFragment(text, context);
                if (anchor != null){
                    return context.addBefore(ktBlockCodeFragment, anchor.getLastChild());
                } else {
                    return context.add(ktBlockCodeFragment);
                }

            }
        });
    }

    /**
     * 创建一个方法
     * @param text
     * @param context
     * @return
     */
    public PsiElement createMethod(String text, PsiElement context, PsiElement anchor){
        return createMethod(text, context, anchor, false);
    }

    /**
     * 创建一个方法
     * @param text
     * @param context
     * @return
     */
    public PsiElement createMethod(String text, PsiElement context, PsiElement anchor, boolean isBefore){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                KtNamedFunction psiMethod = mKtPsiFactory.createFunction(text);
//                Assertion failed: anchorBefore == null || anchorBefore.getTreeParent() == parent
                if (anchor != null){
                    if (isBefore) {
                        return context.addBefore(psiMethod, anchor);
                    } else {
                        return context.addAfter(psiMethod, anchor);
                    }

                } else {
                    return context.add(psiMethod);
                }
            }
        });
    }

    /**
     * 创建并插入Import
     * @param psiJavaFile
     * @return
     */
    public PsiImportStatement createImport(String ktClass, KtFile psiJavaFile){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiImportStatement>() {
            @Override
            public PsiImportStatement compute() {

                FqName fqName = new FqName(ktClass);
                ImportPath importPath = new ImportPath(fqName, false);
                KtImportDirective classImport = mKtPsiFactory.createImportDirective(importPath);

                KtImportList importList = psiJavaFile.getImportList();

                boolean hasImport = false;
                for (KtImportDirective ktImportDirective: importList.getImports()) {
                    if (classImport.getImportPath().equals(ktImportDirective.getImportPath())){
                        hasImport = true;
                        break;
                    }
                }
                if (!hasImport){
                    psiJavaFile.getImportList().add(classImport);
                }

                return null;
            }
        });
    }

    /**
     * 给方法添加CommentDoc
     * @param commentText
     * @param method
     * @return
     */
    public PsiElement createCommentToMethod(String commentText, PsiElement method){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {

                KDocElementFactory kDocElementFactory = new KDocElementFactory(project);
                KDoc kDoc = kDocElementFactory.createKDocFromText(commentText);
//                PsiComment comment = mKtPsiFactory.createComment(commentText);
                return method.addBefore(kDoc, method.getFirstChild());

            }
        });
    }
}
