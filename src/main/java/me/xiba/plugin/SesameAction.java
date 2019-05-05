package me.xiba.plugin;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiElementFactoryImpl;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.PlainTextASTFactory;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import me.xiba.plugin.template.ModelTemplate;
import me.xiba.plugin.utils.ClassSelector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.xiba.plugin.template.ViewModelMethodTomplate.*;

public class SesameAction extends AnAction {

    public static final String KEY_METHOD_NAME = "method_name";
    public static final String KEY_METHOD_RETURN = "method_return";
    public static final String KEY_METHOD_PARAMS = "method_params";
    public static final String KEY_METHOD_COMMENT_DATA = "method_comment_data";

    public static final String KEY_METHOD_PARAMS_WITH_TYPE = "method_params_with_type";
    public static final String KEY_METHOD_PARAMS_WITHOUT_TYPE = "method_params_without_type";

    String name;

    Map<String, Object> sourceMethod;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        // 获取当前的文件
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        // 获取当前文件的目录
        VirtualFile parentDir = file.getParent().getParent();

        name = file.getName().replace("Service.java", "");

        // 生成Model文件内容
        generateModelFile(file, parentDir, e);

        getViewModelFile(project, file);


    }

    /**
     * 生成Model文件内容
     * @param file
     * @param parentDir
     * @param e
     */
    public void generateModelFile(VirtualFile file, VirtualFile parentDir, AnActionEvent e){
        try {
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

            // 解析接口方法
            sourceMethod = parseMethod(getSourceText(e));
            if(modelFile.exists()) {
                // 如果文件存在 向文件添加方法
                modelContent = generatorModelMethod(sourceMethod) + "\n}";

            } else {
                // 如果ModelXxx.java文件
                if(modelDir != null && modelDir.exists()) {
                    // 如果路径不存在，先创建路径
                    modelDir.findOrCreateChildData(file.getUserData(VirtualFile.REQUESTOR_MARKER), modelFileName);
                }
                // 创建Model文件并生成内容
                String packagePath = getPackagePath(e);
                modelContent = generateModelFile(packagePath, name, generatorModelMethod(sourceMethod));

            }

            writeFile(modelFile.getPath(), modelContent);




        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 找到目标文本
     * @param e
     * @return
     */
    private String getSourceText(AnActionEvent e){
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        int line = editor.getCaretModel().getLogicalPosition().line;

        Document document = editor.getDocument();
        int startLine = getStartLine(document, line);
        int endLine = getEndLine(document, line);

        if (startLine == -1 || endLine == -1){
            throw new IllegalArgumentException("未能找到可转化的目标文本");
        }


        String sourceText = getRangeText(document, startLine, endLine);

        return sourceText;
    }


    /**
     * 找到 当前行向上10行，'@'开头的行数
     * @param document
     * @param currentLin
     * @return
     */
    private int getStartLine(Document document, int currentLin){
        int startLine = -1;

        for (int i = 0; i < 10; i++) {
            if (currentLin - i >= 0){

                String lineText = getSelectedText(document, currentLin - i).trim();

                if (lineText.startsWith("@GET") || lineText.startsWith("@POST")
                        || lineText.startsWith("@PUT") || lineText.startsWith("@DELETE")){
                    startLine = currentLin - i;
                    break;
                }
            }

        }

        return startLine;
    }

    /**
     * 找到 当前行向下10行，包含';'的行数
     * @param document
     * @param currentLin
     * @return
     */
    private int getEndLine(Document document, int currentLin){
        int endLine = -1;

        for (int i = 0; i < 10; i++) {

            String lineText = getSelectedText(document, currentLin + i);

            if (lineText.contains(";")){
                endLine = currentLin + i;
                break;
            }

        }

        return endLine;
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
     * 获取两个行数之间的文本
     * @param document
     * @param startLine
     * @param endLine
     * @return
     */
    private String getRangeText(Document document, int startLine, int endLine){

        int lineStart = document.getLineStartOffset(startLine);
        int lineEnd = document.getLineEndOffset(endLine);

        return document.getText(new TextRange(lineStart, lineEnd));
    }

    /**
     * 解析接口方法
     * @param methodStr
     */
    private Map<String, Object> parseMethod(String methodStr){
        if (methodStr == null || methodStr.toString().equals("")){
            return null;
        }

        Map<String, Object> paramMap = new HashMap<>();

        StringBuilder sb = new StringBuilder();


        // 获取返回值
        String returnStartStr = "BaseHttpResult<";
        int indexStartReturn = methodStr.indexOf(returnStartStr);
        int indexEndReturn = methodStr.indexOf("> ");

        String returnStr = methodStr.substring(indexStartReturn + returnStartStr.length(), indexEndReturn - 1);
        // 添加返回值
        paramMap.put(KEY_METHOD_RETURN, returnStr);

        sb.append("returnStr = " + returnStr);
        sb.append("\n");

        // 获取方法名
        String methodNameAndParam = methodStr.substring(indexEndReturn + 1);
        int methodNameEndIndex = methodNameAndParam.indexOf("(");

        String methodNameStr = methodNameAndParam.trim().substring(0, methodNameEndIndex - 1);
        // 添加方法名
        paramMap.put(KEY_METHOD_NAME, methodNameStr);

        sb.append("methodNameStr = " + methodNameStr);
        sb.append("\n");

        // 获取方法参数
        int methodParamEndIndex = methodNameAndParam.indexOf(");");
        String methodParamStr = methodNameAndParam.trim().substring(methodNameEndIndex, methodParamEndIndex - 1);

        String[] methodParams = methodParamStr.split(",");

        List<Pair> paramList = new ArrayList<Pair>();
        if(methodParams.length > 0){
            for (int i = 0; i < methodParams.length; i++) {
                String[] paramCons = methodParams[i].trim().split(" ");

                if (paramCons.length > 1){
                    Pair keyValue = new Pair(paramCons[2], paramCons[1]);

//                    DefaultKeyValue keyValue = new DefaultKeyValue();
                    // 参数名做为key，因为参数名不重复
//                    keyValue.setKey(paramCons[2]);
                    // 参数值做为value
//                    keyValue.setValue(paramCons[1]);

                    paramList.add(keyValue);
                }

            }
        }

        // 添加方法参数
        paramMap.put(KEY_METHOD_PARAMS, paramList);

        return paramMap;
    }

    /**
     * 生成Model中的对应方法
     * @param paramMap
     * @return
     */
    public String generatorModelMethod(Map<String, Object> paramMap){
        if (paramMap == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\tpublic void ");
        sb.append(paramMap.get(KEY_METHOD_NAME));
        sb.append("(");


        List<Pair> paramList = (List<Pair>) paramMap.get(KEY_METHOD_PARAMS);

        StringBuilder params = new StringBuilder();
        if (paramList.size() > 0){
            Pair defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);
                sb.append(defaultKeyValue.getSecond());
                sb.append(" ");
                sb.append(defaultKeyValue.getFirst());
                sb.append(", ");

                params.append(defaultKeyValue.getFirst());
                if(i != paramList.size() - 1){
                    params.append(", ");
                }
            }
        }

        sb.append(String.format("LoadingObserver<%s> loadingObserver", paramMap.get(KEY_METHOD_RETURN)));
        sb.append(") {\n");

        String methodBody = String.format("\t\tObservable observable = getService().%1$s(%2$s);\n", paramMap.get(KEY_METHOD_NAME), params.toString());
        sb.append(methodBody);
        sb.append("\t\tmakeSubscribe(observable, loadingObserver);\n");
        sb.append("\t}");
        sb.append("\n");

        return sb.toString();
    }

    /**
     * 生成ViewModel相关的方法
     * @param methodName 方法名
     * @param returnType 返回值
     * @param paramsWithType 带类型的参数
     * @param params 参数
     * @return
     */
    public void generateViewModelMethod(Project project, PsiFile viewModelPsiFile, String methodName, String returnType, String paramsWithType, String params){

//        String firstLetterUpper = methodName.substring(0,1).toUpperCase().concat(methodName.substring(1));

//        String modelFile = String.format(ViewModelMethodTomplate.METHOD_TEMPLATE, methodName, firstLetterUpper, returnType, paramsWithType, params);
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

        PsiElementFactoryImpl mPsiElementFactoryImpl = new PsiElementFactoryImpl(PsiManagerEx.getInstanceEx(project));



        //生成第一个注释
        String firstCommentText = "// ——————————————————————— ↓↓↓↓ <editor-fold desc=\" method\"> ↓↓↓↓ ——————————————————————— //";
        PsiElement firstCommentElement = createComment(project, mPsiElementFactoryImpl, firstCommentText, lastChild, null);

        // 生成observable变量
        String observableFileText = "private LoadingObserver<%1$s> %2$sObserver;";
        observableFileText = String.format(observableFileText, returnType, methodName);
        PsiElement observableFiledElement = createField(project, mPsiElementFactoryImpl, observableFileText, lastChild, firstCommentElement);


//        PsiElement whiteSpace = createWhiteSpace(project, mPsiElementFactoryImpl, "", lastChild, observableFiledElement);


        // 生成initObservable的方法
        String firstLetterUpper = methodName.substring(0,1).toUpperCase().concat(methodName.substring(1));
        String initObservableMethodText = String.format(INIT_OBSERVER_METHOD_TEMPLATE, methodName, firstLetterUpper, returnType);
        PsiElement initObservableMethodElement = createMethod(project, mPsiElementFactoryImpl, initObservableMethodText, lastChild, observableFiledElement);

        // 生成SuccessEvent变量
        String successEventFiledText = String.format(SUCCESS_EVENT_FIELD_TEMPLATE, methodName, returnType);
        PsiElement successEventFiledElement = createField(project, mPsiElementFactoryImpl, successEventFiledText, lastChild, initObservableMethodElement);

        // 生成getSuccessEvent方法模板
        String getSuccessEventMethodText = String.format(GET_SUCCESS_EVENT_METHOD_TEMPLATE, methodName, firstLetterUpper, returnType);
        PsiElement getSuccessEventMethodElement = createMethod(project, mPsiElementFactoryImpl, getSuccessEventMethodText, lastChild, successEventFiledElement);

        // 生成调用model方法模板
        String modelMethodText = String.format(MODEL_METHOD_TEMPLATE, methodName, paramsWithType, params);
        PsiElement modelMethodElement = createMethod(project, mPsiElementFactoryImpl, modelMethodText, lastChild, getSuccessEventMethodElement);



        // 生成结束注释
//        PsiElement endEditorFoldCommentElement = createComment(project, mPsiElementFactoryImpl, END_EDITOR_FOLD_COMMENT_TEMPLATE, lastChild, modelMethodElement);
//        return modelFile;
    }

    /**
     * 生成model文件内容
     * @param packagePath
     * @param name
     * @param method
     */
    public String generateModelFile(String packagePath, String name, String method){

        String modelFile = String.format(ModelTemplate.MODEL_TEMPLATE, packagePath, name, method);

        return modelFile;

    }

    /**
     * 生成带参数类型的 参数字符串
     * @param paramList
     * @return
     */
    private String generateParamWithType(List<Pair> paramList){
        StringBuilder sb = new StringBuilder();


        if (paramList.size() > 0){
            Pair defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);
                sb.append(defaultKeyValue.getSecond());
                sb.append(" ");
                sb.append(defaultKeyValue.getFirst());

                if(i != paramList.size() - 1){
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 生成参数字符串
     * @param paramList
     * @return
     */
    private String generateParam(List<Pair> paramList){
        StringBuilder params = new StringBuilder();


        if (paramList.size() > 0){
            Pair defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);
                params.append(defaultKeyValue.getFirst());
                if(i != paramList.size() - 1){
                    params.append(", ");
                }
            }
        }
        return params.toString();
    }

    /**
     * 获取service文件的包路径
     * @param e
     * @return
     */
    private String getPackagePath(AnActionEvent e){
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

        Document document = editor.getDocument();
        String firstLineText = getSelectedText(document, 0);

        String startText = "package ";
        int startIndex = firstLineText.indexOf(startText);
        int endIndex = firstLineText.indexOf(".service;");
        return firstLineText.substring(startIndex + startText.length(), endIndex);
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
     * 单选文件
     *
     * @param project
     * @param virtualFile
     */
    private VirtualFile selectSingleVirtualFile(Project project, VirtualFile virtualFile) {
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(); // Single
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        // 选择的文件
        VirtualFile selectedFile = FileChooser.chooseFile(fileChooserDescriptor, project, virtualFile); // Single

        return selectedFile;
//        Messages.showMessageDialog(project, selectedFile.getPath(), "Greeting", Messages.getInformationIcon());
    }

    /**
     * 多选文件
     *
     * @param project
     * @param virtualFile
     */
    private void selectMultiVirtualFiles(Project project, VirtualFile virtualFile) {
        FileChooserDescriptor multipleFilesNoJarsDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(); // Multi
        multipleFilesNoJarsDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile[] selectedFiles = FileChooser.chooseFiles(multipleFilesNoJarsDescriptor, project, virtualFile); // Multi
        Messages.showMessageDialog(project, selectedFiles.length + "", "Greeting", Messages.getInformationIcon());
    }

    /**
     * 创建一个变量
     * @param project
     * @param psiElementFactoryImpl
     * @param text
     * @param context
     * @return
     */
    public PsiElement createField(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context, PsiElement anchor){
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

    /**
     * 创建一个方法
     * @param project
     * @param psiElementFactoryImpl
     * @param text
     * @param context
     * @return
     */
    public PsiElement createMethod(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiMethod psiMethod = psiElementFactoryImpl.createMethodFromText(text, context);
                if (anchor != null){
                    return context.addAfter(psiMethod, anchor);
                } else {
                    return context.add(psiMethod);
                }
            }
        });
    }

    /**
     * 创建一个注释
     * @param project
     * @param psiElementFactoryImpl
     * @param text
     * @param context
     * @return
     */
    public PsiElement createComment(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context, PsiElement anchor){


        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
//                PsiComment comment = psiElementFactoryImpl.createCommentFromText(text, context);
                PsiDocComment comment = psiElementFactoryImpl.createDocCommentFromText("/**\n" +
                        " *\n" +
                        " */");

                if (anchor != null){
                    return context.addAfter(comment, anchor);
                } else {
                    return context.add(comment);
                }
            }
        });
    }

    public PsiElement createWhiteSpace(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiWhiteSpaceImpl psiWhiteSpace = new PsiWhiteSpaceImpl("/n");

                if (anchor != null){
                    return context.addAfter(psiWhiteSpace, anchor);
                } else {
                    return context.add(psiWhiteSpace);
                }
            }
        });
    }

    public PsiElement createTextCommen(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context, PsiElement anchor){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PlainTextASTFactory factory = new PlainTextASTFactory();
                LeafElement leafElement = factory.createLeaf(PlainTextTokenTypes.PLAIN_TEXT, text + "\n");
                ASTNode node = context.getNode();

                ASTNode[] children = node.getChildren(null);



                node.addChild(leafElement, children[children.length - 2]);

//                PsiPlainTextImpl psiComment = new PsiPlainTextImpl(text);
//                DummyHolderFactory.createHolder(PsiManagerEx.getInstanceEx(project), (TreeElement) SourceTreeToPsiMap.psiElementToTree(psiComment), context);
//                PsiElement comment = psiElementFactoryImpl.createCommentFromText(text, null);


//                if (anchor != null){
//                    return context.addBefore(psiComment, anchor);
//                } else {
//                    return context.add(psiComment);
//                }
                return null;
            }
        });
    }
//    createDocTagFromText

    /**
     * 创建一个注释
     * @param project
     * @param psiElementFactoryImpl
     * @param text
     * @param context
     * @return
     */
    public PsiElement createComment(Project project, PsiElementFactoryImpl psiElementFactoryImpl, String text, PsiElement context){
        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                PsiComment comment = psiElementFactoryImpl.createCommentFromText(text, context);
                return context.add(comment);
            }
        });
    }

    /**
     * 删除一个元素
     * @param project
     * @param context
     * @return
     */
    public PsiElement deletePsiElement(Project project, PsiElement context){


        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                context.delete();
                return null;
            }
        });
    }

    /**
     * 添加一个元素
     * @param project
     * @param context
     * @return
     */
    public PsiElement addPsiElement(Project project, PsiElement context, PsiElement element){


        return WriteCommandAction.runWriteCommandAction(project, new Computable<PsiElement>() {
            @Override
            public PsiElement compute() {
                return context.add(element);
            }
        });
    }


    /**
     * 获取ViewModel文件夹下的所有文件
     *
     * @param virtualFile
     */
    private void getViewModelFile(Project project, VirtualFile virtualFile) {
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

        String methodName = (String) sourceMethod.get(KEY_METHOD_NAME);
        String returnType = (String) sourceMethod.get(KEY_METHOD_RETURN);
        List<Pair> paramList = (List<Pair>) sourceMethod.get(KEY_METHOD_PARAMS);

        for (VirtualFile viewModelFile : resultFileList) {
            // 找到ViewModel对应的PsiFile
            PsiFile viewModelPsiFile = PsiManager.getInstance(project).findFile(viewModelFile);
            // 向ViewModel中插入相关的方法
            generateViewModelMethod(project, viewModelPsiFile, methodName, returnType, generateParamWithType(paramList), generateParam(paramList));
        }

    }







}




