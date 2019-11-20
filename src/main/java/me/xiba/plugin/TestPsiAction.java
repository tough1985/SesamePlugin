package me.xiba.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import me.xiba.plugin.template.ModelTemplate;
import me.xiba.plugin.utils.ClassSelector;
import me.xiba.plugin.utils.KtPsiCreator;
import me.xiba.plugin.utils.MethodPaser;
import me.xiba.plugin.utils.PsiCreator;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import static me.xiba.plugin.SesameAction.*;
import static me.xiba.plugin.template.ViewModelMethodTomplate.*;

/**
 * 1. 解析当前光标所在的方法，保存方法名称，参数名和对应的类型，返回值类型，注释
 */
public class TestPsiAction extends AnAction {

    String name;
    Map<String, Object> sourceMethod;
    PsiCreator psiCreator;
    KtPsiCreator ktPsiCreator;
    Project project;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();

        // 当前光标所在的PsiFile
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);

        // 当前光标所在的PsiElement
        PsiElement psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT);

        // 获取当前的文件
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        PsiElementFactoryImpl mPsiElementFactoryImpl = new PsiElementFactoryImpl(PsiManagerEx.getInstanceEx(project));
        psiCreator = new PsiCreator(project, mPsiElementFactoryImpl);
        name = file.getName().replace("Service.java", "");

        System.out.println("file.getName()=" + file.getName());
        System.out.println("virtualFile.getCanonicalPath()=" + virtualFile.getCanonicalPath());

        KtPsiFactory mKtPsiFactory = new KtPsiFactory(project);
        ktPsiCreator = new KtPsiCreator(project, mKtPsiFactory);

        // 1. 解析当前光标所在的方法
        if (file.getName().endsWith(".kt")){
            sourceMethod = MethodPaser.parseKtCurrenMethod(psiElement);
        } else {
            sourceMethod = MethodPaser.parseCurrenMethod(psiElement);
        }

        if (sourceMethod == null){
            return;
        }

        // 2. 生成Model文件
        generateModelFile(project, e);

        // 3. 显示选择面板
        getViewModelFile(virtualFile);

    }


    /**
     * 生成Model文件内容
     * @param e
     */
    public void generateModelFile(Project project,  AnActionEvent e){
        try {
            // 获取当前的文件
            VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
            // 获取当前文件的目录
            VirtualFile parentDir = file.getParent().getParent();

            //创建「model」文件夹
            VirtualFile modelDir = parentDir.findChild("model");
            if (modelDir == null){
                modelDir = parentDir.createChildDirectory(file.getUserData(VirtualFile.REQUESTOR_MARKER), "model");
            }

            //获取「model」文件名称
            String modelFileName = file.getName().replace("Service", "Model");

            // 创建「model」文件
            String modelFileFullName = modelDir.getPath() + "/" + modelFileName;
            File modelFile = new File(modelFileFullName);

            String modelContent;
            boolean isServiceKt = name.endsWith(".kt");

            // 创建Model文件并生成内容
            if(!modelFile.exists()) {

                // 如果ModelXxx.java文件
                if(modelDir != null && !modelDir.exists()) {
                    // 如果路径不存在，先创建路径
                    modelDir.findOrCreateChildData(file.getUserData(VirtualFile.REQUESTOR_MARKER), modelFileName);
                }

                String packagePath = getPackagePath(e, isServiceKt);

                modelContent = generateModelFile(packagePath, name, generatorModelMethod(isServiceKt));

                writeFile(modelFile.getPath(), modelContent);

            } else {    // 已经有Model文件，添加相关方法

                VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(modelFile);
                PsiFile viewModelPsiFile = PsiManager.getInstance(project).findFile(virtualFile);

                PsiElement lastChild = viewModelPsiFile.getLastChild();

                PsiElement[] children = viewModelPsiFile.getChildren();

                for (int i = 0; i < children.length; i++) {
                    int index = children.length - 1 - i;
                    if (children[index] instanceof PsiWhiteSpace){
                        continue;
                    }
                    lastChild = children[index];
                    break;
                }

                if (isServiceKt) {
                    ktPsiCreator.createMethod(generatorModelMethod(isServiceKt),
                            (KtClass)lastChild,
                            lastChild.getLastChild().getLastChild(),
                            true);
                } else {
                    psiCreator.createMethod(
                            generatorModelMethod(isServiceKt),
                            lastChild,
                            lastChild.getLastChild().getLastChild(),
                            true);
                }

                if(isServiceKt) {
                    // 生成返回类型import代码
                    generateReturnAndParamImport((KtFile) viewModelPsiFile);
                } else {
                    // 生成返回类型import代码
                    generateReturnAndParamImport((PsiJavaFileImpl) viewModelPsiFile);
                }

            }


        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 生成Model方法的字符串
     */
    public String generatorModelMethod(boolean isModelKt){

        String methodName = (String) sourceMethod.get(KEY_METHOD_NAME);

        String commentData = (String) sourceMethod.get(KEY_METHOD_COMMENT_DATA);

        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);

        String paramsWithoutType = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITHOUT_TYPE);

        String paramsWithType = isModelKt ? (String) sourceMethod.get(KEY_METHOD_PARAMS_WITH_TYPE_KT) : (String) sourceMethod.get(KEY_METHOD_PARAMS_WITH_TYPE);

        String modelTemplate = isModelKt ? MODEL_METHOD_KT_TMPLATE : MODEL_METHOD_TMPLATE;
//

        // 1 方法名  %1$s
        // 2 带类型的参数  %2$s
        // 3 注释  %3$s
        // 4 返回值  %4$s
        // 5 不带类型的参数  %5$s
        return String.format(modelTemplate, methodName, paramsWithType, commentData, returnType, paramsWithoutType);

    }

    private void writeFile(String file, String content) throws IOException {

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            if (randomAccessFile.length() > 2){
                randomAccessFile.seek(randomAccessFile.length() - 2);
            }
            randomAccessFile.write(content.getBytes("UTF-8"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            randomAccessFile.close();
        }

    }

    /**
     * 生成model文件内容
     * @param packagePath
     * @param name
     * @param method
     */
    public String generateModelFile(String packagePath, String name, String method){

        // 判断service文件是不是kotlin
        boolean isServiceKt = name.endsWith(".kt");

        String modelName;

        if (isServiceKt){
            modelName = name.replace("Service.kt", "");
        } else {
            modelName = name.replace("Service.java", "");
        }

        // 如果Service是kotlin文件，生成的model也是kotlin
        String template = isServiceKt? ModelTemplate.KT_MODEL_TEMPLATE : ModelTemplate.MODEL_TEMPLATE;

        String modelFile = String.format(template, packagePath, modelName, method);

        return modelFile;

    }

    /**
     * 获取service文件的包路径
     * @param e
     * @return
     */
    private String getPackagePath(AnActionEvent e, boolean isKt){
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

        Document document = editor.getDocument();
        String firstLineText = getSelectedText(document, 0);

        String startText = "package ";
        int startIndex = firstLineText.indexOf(startText);
        int endIndex;
        if(isKt){
            endIndex = firstLineText.indexOf(".service");
        } else{
            endIndex = firstLineText.indexOf(".service;");
        }

        return firstLineText.substring(startIndex + startText.length(), endIndex);
    }

    /**
     * 根据行数，获取当前行的文本
     * @param document
     * @param line
     * @return
     */
    private String getSelectedText(Document document, int line){

        int lineStart = document.getLineStartOffset(line);
        int lineEnd = document.getLineEndOffset(line);

        return document.getText(new TextRange(lineStart, lineEnd));
    }

    /**
     * 生成import代码
     * @param className
     * @param psiJavaFile
     */
    public void generateImport(String className, PsiJavaFileImpl psiJavaFile){
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);

        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(className, searchScope);

        if (psiClasses.length > 0){

            psiCreator.createImport(psiClasses[0], psiJavaFile);

        }
    }

    /**
     * 生成import代码
     * @param className
     * @param psiJavaFile
     */
    public void generateImport(String className, KtFile psiJavaFile){
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);


        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(className, searchScope);

        if (psiClasses.length > 0){

            ktPsiCreator.createImport(psiClasses[0].getQualifiedName(), psiJavaFile);

        }
    }

    public void generateReturnAndParamImport(PsiJavaFileImpl psiJavaFile){
        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);

        generateImport(returnType, psiJavaFile);

        List<Pair> paramList = (List<Pair>) sourceMethod.get(KEY_METHOD_PARAMS);

        if (paramList.size() > 0){
            Pair defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);

                String paramsType = (String) defaultKeyValue.getSecond();

                boolean isLower = paramsType.charAt(0) >= 97 && paramsType.charAt(0) <= 122;

                if (paramsType != null
                        && !isLower
                        && !paramsType.equals("String")){
                    generateImport(paramsType, psiJavaFile);
                }

            }
        }
    }

    public void generateReturnAndParamImport(KtFile psiJavaFile){
        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);

        generateImport(returnType, psiJavaFile);

        List<Pair> paramList = (List<Pair>) sourceMethod.get(KEY_METHOD_PARAMS);

        if (paramList.size() > 0){
            Pair defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);

                String paramsType = (String) defaultKeyValue.getSecond();

                boolean isLower = paramsType.charAt(0) >= 97 && paramsType.charAt(0) <= 122;

                if (paramsType != null
                        && !isLower
                        && !paramsType.equals("String")){
                    generateImport(paramsType, psiJavaFile);
                }

            }
        }
    }

    /**
     * 获取ViewModel文件夹下的所有文件
     *
     * @param virtualFile
     */
    private void getViewModelFile(VirtualFile virtualFile) {
        VirtualFile parentDir = virtualFile.getParent().getParent();
        VirtualFile vmDir = parentDir.findChild("viewmodel");
        VirtualFile[] vmFiles;
        if (vmDir != null && vmDir.exists()) {
            vmFiles = vmDir.getChildren();
            if (vmFiles.length > 0) {
                showSelectPanel(project, vmFiles);
            }
        }
    }

    /**
     * 显示VM选择面板
     */
    private void showSelectPanel(Project project, VirtualFile[] vmFiles) {
        ClassSelector classSelector = new ClassSelector(vmFiles);
        classSelector.setOnFileSelectListener(new ClassSelector.OnFileSelectListener() {
            @Override
            public void onFileSelected(List<VirtualFile> list) {
                onViewModelFileSelected(project, list);
            }
        });
        classSelector.setVisible(true);
    }

    /**
     * VM选择完成执行
     *
     * @param resultFileList
     */
    private void onViewModelFileSelected(Project project, List<VirtualFile> resultFileList) {

        for (VirtualFile viewModelFile : resultFileList) {

            // 判断选中文件是不是kotlin
            boolean isKt = viewModelFile.getName().endsWith(".kt");

            // 找到ViewModel对应的PsiFile
            PsiFile viewModelPsiFile = PsiManager.getInstance(project).findFile(viewModelFile);

            // 向ViewModel中插入相关的方法
            if (isKt){
                generateKtVMCode(viewModelPsiFile);
            } else {
                generateVMCode(viewModelPsiFile);
            }

        }

    }

    /**
     * 生成VM相关代码
     * @param viewModelPsiFile
     */
    public void generateVMCode(PsiFile viewModelPsiFile){
        PsiElement lastChild = viewModelPsiFile.getLastChild();

        PsiElement[] children = viewModelPsiFile.getChildren();

        for (int i = 0; i < children.length; i++) {
            int index = children.length - 1 - i;
            if (children[index] instanceof PsiWhiteSpace){
                continue;
            }
            lastChild = children[index];
            break;
        }

        String methodName = (String) sourceMethod.get(KEY_METHOD_NAME);

        String commentData = (String) sourceMethod.get(KEY_METHOD_COMMENT_DATA);

        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);

        String paramsWithoutType = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITHOUT_TYPE);

        String paramsWithType = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITH_TYPE);

        // step 1 创建一个变量
        String observableFileText = "private LoadingObserver<%1$s> %2$sObserver;";
        observableFileText = String.format(observableFileText, returnType, methodName);
        PsiElement field = psiCreator.createField(observableFileText, lastChild, lastChild);

        // step 2 创建变量的注释
        String commentText = "/**\n" + " * 「%1$s」\n" + "*/";
        commentText = String.format(commentText, commentData);
        PsiElement comment = psiCreator.createCommentDocToMethod(commentText, field);

        // step 3 创建代码块起始注释
        String codeStartComment = "// ——————————————————————— ↓↓↓↓ <editor-fold desc=\"「%1$s」 method\"> ↓↓↓↓ ——————————————————————— //";
        codeStartComment = String.format(codeStartComment, commentData);
        PsiElement codeStartCommentPsi = psiCreator.createComment(codeStartComment, lastChild, field, true);

        // step 4 生成initObservable的方法
        String firstLetterUpper = methodName.substring(0,1).toUpperCase().concat(methodName.substring(1));
        String initObservableMethodText = String.format(INIT_OBSERVER_METHOD_TEMPLATE, methodName, firstLetterUpper, returnType);
        PsiElement initObservableMethodElement = psiCreator.createMethod(initObservableMethodText, lastChild, field);

        // step 5 创建方法的注释
        String commentInitMethodText = "/**\n" + " * 初始化「%1$s」\n" + "*/";
        commentInitMethodText = String.format(commentInitMethodText, commentData);
        PsiElement commentInitMethod = psiCreator.createCommentDocToMethod(commentInitMethodText, initObservableMethodElement);

        // step 6 生成SuccessEvent变量
        String successEventFiledText = String.format(SUCCESS_EVENT_FIELD_TEMPLATE, methodName, returnType);
        PsiElement successEventFiledElement = psiCreator.createFieldAfter(successEventFiledText, lastChild, initObservableMethodElement);

        // step 7 创建方法的注释
        String commentSuccessEventText = "/**\n" + " *「%1$s」成功\n" + "*/";
        commentSuccessEventText = String.format(commentSuccessEventText, commentData);
        PsiElement commentSuccessEvent = psiCreator.createCommentDocToMethod(commentSuccessEventText, successEventFiledElement);

        // step 8 生成调用model方法模板
        String modelMethodText = String.format(MODEL_METHOD_TEMPLATE, methodName, paramsWithType, paramsWithoutType);
        PsiElement modelMethodElement = psiCreator.createMethod(modelMethodText, lastChild, successEventFiledElement);

        // step 9 创建方法的注释
        String commentModelMethodText = "/**\n" + " * 调用「%1$s」接口\n" + "*/";
        commentModelMethodText = String.format(commentModelMethodText, commentData);
        PsiElement commentModelMethod = psiCreator.createCommentDocToMethod(commentModelMethodText, modelMethodElement);

        // step 10 创建代码块终止注释
        PsiElement codeEndCommentPsi = psiCreator.createComment(END_EDITOR_FOLD_COMMENT_TEMPLATE, lastChild, modelMethodElement, false);

        // 生成返回类型import代码
        generateReturnAndParamImport((PsiJavaFileImpl) viewModelPsiFile);

    }


    /**
     * 生成VM相关代码
     * @param viewModelPsiFile
     */
    public void generateKtVMCode(PsiFile viewModelPsiFile){
        PsiElement lastChild = viewModelPsiFile.getLastChild();

        PsiElement[] children = viewModelPsiFile.getChildren();

        for (int i = 0; i < children.length; i++) {
            int index = children.length - 1 - i;
            if (children[index] instanceof PsiWhiteSpace){
                continue;
            }
            lastChild = children[index];
            break;
        }

        String methodName = (String) sourceMethod.get(KEY_METHOD_NAME);

        String commentData = (String) sourceMethod.get(KEY_METHOD_COMMENT_DATA);

        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);

        String paramsWithoutType = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITHOUT_TYPE_KT);

        String paramsWithTypeKt = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITH_TYPE_KT);

        String paramsWithType = (String) sourceMethod.get(KEY_METHOD_PARAMS_WITH_TYPE);

        if (!paramsWithoutType.trim().equals("")){
            paramsWithoutType = paramsWithoutType + ", ";
        }

        if (commentData.contains("*")){
            int startIndex = commentData.indexOf("*") + 1;
            int endIndex = commentData.indexOf("\n");
            if (endIndex == -1){
                endIndex = commentData.length();
            }
            if (startIndex < endIndex){
                commentData = commentData.substring(startIndex, endIndex).trim();
            }
        }

        // step 1 创建一个LoadingObserver变量
        String observableFileText;
        observableFileText = String.format(KT_VIEW_MODEL_TEMPLATE, methodName, returnType, commentData, paramsWithTypeKt, paramsWithoutType);
        PsiElement field = ktPsiCreator.createField(observableFileText, lastChild, lastChild.getLastChild());

        // 生成返回类型import代码
        generateReturnAndParamImport((KtFile) viewModelPsiFile);
    }


}

