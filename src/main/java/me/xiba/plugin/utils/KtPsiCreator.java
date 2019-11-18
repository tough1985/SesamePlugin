package me.xiba.plugin.utils;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtPsiFactory;

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
                KtParameter psiField = mKtPsiFactory.createParameter(text);

                if (anchor != null){
                    return context.addBefore(psiField, anchor.getLastChild());
                } else {
                    return context.add(psiField);
                }

            }
        });
    }
}
