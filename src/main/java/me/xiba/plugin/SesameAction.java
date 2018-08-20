package me.xiba.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import me.xiba.plugin.template.ModelTemplate;
import org.apache.commons.collections.keyvalue.DefaultKeyValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SesameAction extends AnAction {

    public static final String KEY_METHOD_NAME = "method_name";
    public static final String KEY_METHOD_RETURN = "method_return";
    public static final String KEY_METHOD_PARAMS = "method_params";


    String name;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();

        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        String filePath = "";

        filePath = file.getPath();

        StringBuilder sb = new StringBuilder();
        sb.append("filePath=" + filePath);
        sb.append("\n");

        VirtualFile parentDir = file.getParent().getParent();
        String fileParentPath = parentDir.getPath();
        sb.append("fileParentPath=" + fileParentPath);
        sb.append("\n");
        sb.append("file.getName()=" + file.getName());
        sb.append("\n");
        sb.append("threadName=" + Thread.currentThread().getName());
        sb.append("\n");

        name = file.getName().replace("Service.java", "");

        try {
            //创建「model」文件夹
            VirtualFile modelDir = parentDir.findChild("model");
            if (modelDir == null){
                modelDir = parentDir.createChildDirectory(file.getUserData(VirtualFile.REQUESTOR_MARKER), "model");
            }

            //获取「model」文件名称
            String modelFileName = file.getName().replace("Service", "Model");




            String modelFileFullName = modelDir.getPath() + "/" + modelFileName;
            File modelFile = new File(modelFileFullName);



            String modelContent;
            if(modelFile.exists()) {
                modelContent = generatorModelMethod(parseMethod(getSourceText(e))) + "\n}";



            } else {
                // 如果ModelXxx.java文件
                if(modelDir != null && modelDir.exists()) {
                    modelDir.findOrCreateChildData(file.getUserData(VirtualFile.REQUESTOR_MARKER), modelFileName);
                }
                String packagePath = getPackagePath(e);
                modelContent = generateModelFile(packagePath, name, generatorModelMethod(parseMethod(getSourceText(e))));

            }


            RandomAccessFile randomAccessFile = new RandomAccessFile(modelFileFullName, "rw");

            if (randomAccessFile.length() > 2){
                randomAccessFile.seek(randomAccessFile.length() - 2);
            }

            randomAccessFile.write(modelContent.getBytes("UTF-8"));

            randomAccessFile.close();

            sb.append("modelFileFullName=" + modelFileFullName);




        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Messages.showMessageDialog(project, sb.toString(), "Greeting", Messages.getInformationIcon());
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

        List<DefaultKeyValue> paramList = new ArrayList<DefaultKeyValue>();
        if(methodParams.length > 0){
            for (int i = 0; i < methodParams.length; i++) {
                String[] paramCons = methodParams[i].trim().split(" ");

                if (paramCons.length > 1){
                    DefaultKeyValue keyValue = new DefaultKeyValue();
                    // 参数名做为key，因为参数名不重复
                    keyValue.setKey(paramCons[2]);
                    // 参数值做为value
                    keyValue.setValue(paramCons[1]);

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

        List<DefaultKeyValue> paramList = (List<DefaultKeyValue>) paramMap.get(KEY_METHOD_PARAMS);

        StringBuilder params = new StringBuilder();
        if (paramList.size() > 0){
            DefaultKeyValue defaultKeyValue;
            for (int i = 0; i < paramList.size(); i++) {
                defaultKeyValue = paramList.get(i);
                sb.append(defaultKeyValue.getValue());
                sb.append(" ");
                sb.append(defaultKeyValue.getKey());
                sb.append(", ");

                params.append(defaultKeyValue.getKey());
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

}
