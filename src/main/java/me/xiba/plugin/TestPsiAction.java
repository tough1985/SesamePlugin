package me.xiba.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;

import java.lang.reflect.Method;

public class TestPsiAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();

        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);



        PsiJavaFileImpl psiJavaFile = (PsiJavaFileImpl) file;

        PsiElement[] elements = psiJavaFile.getChildren();
        PsiElement lastChild = psiJavaFile.getLastChild();

        System.out.println("lastChild.getText()=" + lastChild.getText());


        PsiElementFactoryImpl mPsiElementFactoryImpl = new PsiElementFactoryImpl(PsiManagerEx.getInstanceEx(project));

        String methodText = "public SingleLiveEvent<Object> getGetCertificationResultSuccessEvent() {\n" +
                "        return getCertificationResultSuccessEvent;\n" +
                "    }";

        PsiElement mPsiElement = WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiField psiField = mPsiElementFactoryImpl.createFieldFromText("private LoadingObserver<Object> getCertificationResultObsearver;", lastChild);

                return lastChild.add(psiField);
            }
        });

        WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiMethod psiMethod = mPsiElementFactoryImpl.createMethodFromText(methodText, lastChild);
                return lastChild.add(psiMethod);
            }
        });



//        ApplicationManager.getApplication().runWriteAction(new Runnable() {
//            @Override
//            public void run() {
//                PsiField psiField = mPsiElementFactoryImpl.createFieldFromText("private LoadingObserver<Object> getCertificationResultObsearver;", lastChild);
//                lastChild.add(psiField);
//            }
//        });
    }
}
