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

import java.io.FileWriter;
import java.io.IOException;

public class SesameAction extends AnAction {

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

        try {
            //创建「model」文件夹
            VirtualFile modelDir = parentDir.findChild("model");
            if (modelDir == null){
                modelDir = parentDir.createChildDirectory(file.getUserData(VirtualFile.REQUESTOR_MARKER), "model");
            }

            //获取「model」文件名称
            String modelFileName = file.getName().replace("Service", "Model");

            // 如果ModelXxx.java文件
            if(modelDir != null && modelDir.exists()) {
                modelDir.findOrCreateChildData(file.getUserData(VirtualFile.REQUESTOR_MARKER), modelFileName);
            }


            String modelFileFullName = modelDir.getPath() + "/" + modelFileName;

            // 写文件
            FileWriter fileWriter = new FileWriter(modelFileFullName);
            fileWriter.write(getSourceText(e));
            fileWriter.close();
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
}
