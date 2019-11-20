package me.xiba.plugin.utils;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.javadoc.PsiDocComment;

public class PsiCreator {

    private Project project;
    private PsiElementFactoryImpl psiElementFactoryImpl;

    public PsiCreator(Project project, PsiElementFactoryImpl psiElementFactoryImpl) {
        this.project = project;
        this.psiElementFactoryImpl = psiElementFactoryImpl;
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
                PsiField psiField = psiElementFactoryImpl.createFieldFromText(text, context);

                if (anchor != null){
                    return context.addBefore(psiField, anchor.getLastChild());
                } else {
                    return context.add(psiField);
                }

            }
        });
    }

    /**
     * 创建一个变量
     * @param text
     * @param context
     * @return
     */
    public PsiElement createFieldAfter(String text, PsiElement context, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiField psiField = psiElementFactoryImpl.createFieldFromText(text, context);

                if (anchor != null){
                    return context.addAfter(psiField, anchor);
                } else {
                    return context.add(psiField);
                }

            }
        });
    }

    public PsiElement createWhiteSpace(PsiElement context, PsiElement anchor, boolean isBefore){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiElement whiteSpace = PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n");
                if (isBefore){
                    return context.addBefore(whiteSpace, anchor);
                } else {
                    return context.addAfter(whiteSpace, anchor);
                }

            }
        });
    }

    /**
     * 给方法添加CommentDoc
     * @param commentText
     * @param method
     * @return
     */
    public PsiElement createCommentDocToMethod(String commentText, PsiElement method){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {

                PsiDocComment comment = psiElementFactoryImpl.createDocCommentFromText(commentText);
                return method.addBefore(comment, method.getFirstChild());

            }
        });
    }

    /**
     * 给方法添加注释
     * @param commentText
     * @param conotext
     * @param anchor
     * @return
     */
    public PsiElement createCommentToMethod(String commentText, PsiElement conotext, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiElement comment = psiElementFactoryImpl.createCommentFromText(commentText, conotext);
                return anchor.addBefore(comment, anchor.getFirstChild());

            }
        });
    }

    /**
     * 添加注释
     * @param commentText
     * @param context
     * @param anchor
     * @return
     */
    public PsiElement createComment(String commentText, PsiElement context, PsiElement anchor, boolean isBefore){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiElement comment = psiElementFactoryImpl.createCommentFromText(commentText, context);
                if (isBefore){
                    return context.addBefore(comment, anchor);
                } else {
                    return context.addAfter(comment, anchor);
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
                PsiMethod psiMethod = psiElementFactoryImpl.createMethodFromText(text, context);
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
     * @param psiClass
     * @param psiJavaFile
     * @return
     */
    public PsiImportStatement createImport(PsiClass psiClass, PsiJavaFileImpl psiJavaFile){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiImportStatement>() {
            @Override
            public PsiImportStatement compute() {
                PsiImportStatement importStatement = psiElementFactoryImpl.createImportStatement(psiClass);
                PsiImportList importList = psiJavaFile.getImportList();

                if (importList.findSingleImportStatement(psiClass.getName()) == null){
                    psiJavaFile.getImportList().add(importStatement);
                }

                return importStatement;
            }
        });
    }
}
