package me.xiba.plugin.utils;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassSelector extends JFrame {

    private OnFileSelectListener onFileSelectListener;

    private Map<String, VirtualFile> vmMap = new HashMap<>();
    private java.util.List<VirtualFile> resultFileList = new ArrayList<>();
    private List<String> selectedClass = new ArrayList<>();

    public ClassSelector(VirtualFile[] virtualFiles) {
        setLayout(null);
        setTitle("Select Target Class");
        setPreferredSize(new Dimension(800, 500));
        setBounds(700, 200, 800, 500); // 弹窗位置
        setResizable(false);


        JButton jButton = new JButton("Confirm");
        jButton.setBounds(10, 420, 780, 40);
        jButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedClass.size() > 0) {
                    for (String str : selectedClass) {
                        resultFileList.add(vmMap.get(str));
                    }
                    if (onFileSelectListener != null) {
                        onFileSelectListener.onFileSelected(resultFileList);
                    }
                }
                if (isShowing()) {
                    dispose();
                }
            }
        });
        add(jButton);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setBounds(10, 10, 780, 400);
//        contentPanel.setBackground(Color.ORANGE);
        add(contentPanel);

        JLabel vmTitle = new JLabel("Select target viewModel");
//        vmTitle.setForeground(Color.BLACK);
        vmTitle.setBounds(0, 0, 780, 30);
        contentPanel.add(vmTitle);


        JPanel panel = new JPanel();
//        panel.setBackground(Color.green);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setLayout(new BorderLayout(0, 0));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(780, 350));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBounds(0, 40, 780, 350);

        scrollPane.setViewportView(createCheckListPanel2(virtualFiles));
        contentPanel.add(panel);

    }

    private JPanel createCheckListPanel(VirtualFile[] virtualFiles) {
        int itemCount = virtualFiles.length;

        JPanel panel = new JPanel(new GridLayout(itemCount, 1, 10, 10));
        panel.setPreferredSize(new Dimension(200, itemCount * 40));
//        for (int i = 0; i < 10; i++) {
        for (int i = 0; i < itemCount; i++) {
            vmMap.put(virtualFiles[i].getName(), virtualFiles[i]);

            JCheckBox checkBox = new JCheckBox(virtualFiles[i].getName());
            checkBox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JCheckBox view = (JCheckBox) e.getSource();
                    if (view.isSelected() && !selectedClass.contains(view.getText())) {
                        selectedClass.add(view.getText());
                    } else if (!view.isSelected() && selectedClass.contains(view.getText())) {
                        selectedClass.remove(view.getText());
                    }
                }
            });

            panel.add(checkBox);
        }
        return panel;
    }

    private JPanel createCheckListPanel2(VirtualFile[] virtualFiles) {
        int itemCount = virtualFiles.length;

        int startY = 10;

        int panelHeight = 330;
        if (virtualFiles.length * 35 > panelHeight) {
            panelHeight = virtualFiles.length * 35;
        }
        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(600, panelHeight));
//        for (int i = 0; i < 10; i++) {
        for (int i = 0; i < itemCount; i++) {
            vmMap.put(virtualFiles[i].getName(), virtualFiles[i]);

            JCheckBox checkBox = new JCheckBox(virtualFiles[i].getName());
            checkBox.setBounds(10, startY, 580, 30);
            startY += 30;
            checkBox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JCheckBox view = (JCheckBox) e.getSource();
                    if (view.isSelected() && !selectedClass.contains(view.getText())) {
                        selectedClass.add(view.getText());
                    } else if (!view.isSelected() && selectedClass.contains(view.getText())) {
                        selectedClass.remove(view.getText());
                    }
                }
            });

            panel.add(checkBox);
        }
        return panel;
    }

    /**
     * 设置文件选择监听
     *
     * @param listener
     */
    public void setOnFileSelectListener(OnFileSelectListener listener) {
        onFileSelectListener = listener;
    }

    public interface OnFileSelectListener {
        void onFileSelected(java.util.List<VirtualFile> list);
    }

//    public static void main(String[] args) {
//        ClassSelector classSelector = new ClassSelector();
//        classSelector.setVisible(true);
//    }

}

