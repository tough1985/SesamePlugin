package me.xiba.plugin.utils;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtTypeParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.xiba.plugin.SesameAction.*;
import static me.xiba.plugin.SesameAction.KEY_METHOD_PARAMS;
import static me.xiba.plugin.SesameAction.KEY_METHOD_PARAMS_WITHOUT_TYPE;

public class MethodPaser {

    /**
     * 解析当前光标所在的方法
     */
    public static Map<String, Object> parseKtCurrenMethod(PsiElement psiElement){

        if (psiElement instanceof KtNamedFunction){
            Map<String, Object> paramMap = new HashMap<>();
            KtNamedFunction currentMethod = (KtNamedFunction)psiElement;

            // 添加方法名
            paramMap.put(KEY_METHOD_NAME, currentMethod.getName());
            System.out.println("currentMethod.getName()=" + currentMethod.getName());

            System.out.println("currentMethod.hasDeclaredReturnType()=" + currentMethod.hasDeclaredReturnType());

            if (currentMethod.getTypeReference() != null){
                System.out.println("currentMethod.getTypeReference().getText()=" + currentMethod.getTypeReference().getText());

            }

            // 获取返回值
            String methodStr = currentMethod.getTypeReference().getText();

            String returnStartStr = "BaseHttpResult<";
            int indexStartReturn = methodStr.indexOf(returnStartStr);
            int indexEndReturn = methodStr.lastIndexOf(">");

            String returnStr = methodStr.substring(indexStartReturn + returnStartStr.length(), indexEndReturn - 1);

            System.out.println("returnStr=" + returnStr);
            // 添加返回值
            paramMap.put(KEY_METHOD_RETURN, returnStr);

            // doc
            for (int i = 0; i < currentMethod.getDocComment().getChildren().length; i++) {
                System.out.println(i + ": doc=" + currentMethod.getDocComment().getChildren()[i].getText());

                PsiElement item = currentMethod.getDocComment().getChildren()[i];

                if (item.getText() != null && !item.getText().trim().equals("")
                        && !item.getText().trim().equals("/**")
                        && !item.getText().trim().equals("*")){
                    // 添加注释
                    paramMap.put(KEY_METHOD_COMMENT_DATA, item.getText().trim());
                    break;
                }
            }

            int paramCount = currentMethod.getValueParameters().size();

            StringBuilder params = new StringBuilder();
            StringBuilder paramsWithoutType = new StringBuilder();

            List<Pair> paramList = new ArrayList<Pair>();

            if(paramCount > 0){
                for (int i = 0; i < paramCount; i++) {
                    KtParameter parameter = currentMethod.getValueParameters().get(i);


                    String paramType = "";
                    if (parameter.getChildren().length > 1){
                        paramType = parameter.getChildren()[1].getText();
                    }
                    // 参数名做为key，因为参数名不重复
                    // 参数值做为value
                    Pair keyValue = new Pair(parameter.getName(), paramType);
//
                    paramList.add(keyValue);
//
                    params.append(paramType);
                    params.append(" ");
                    params.append(parameter.getName());


                    paramsWithoutType.append(parameter.getName());
                    if(i != paramCount - 1){
                        params.append(", ");
                        paramsWithoutType.append(", ");
                    }

                    System.out.println(i + ": parameter.getName()=" + parameter.getName());
                    System.out.println(i + ": paramType=" + paramType);

                }
            }

            // 添加方法参数字符串
            paramMap.put(KEY_METHOD_PARAMS_WITH_TYPE, params.toString());
            paramMap.put(KEY_METHOD_PARAMS_WITHOUT_TYPE, paramsWithoutType.toString());
            // 添加方法参数
            paramMap.put(KEY_METHOD_PARAMS, paramList);

        }

        return null;
    }

    /**
     * 解析当前光标所在的方法
     */
    private Map<String, Object> parseCurrenMethod(PsiElement psiElement){

        if (psiElement instanceof PsiMethod){
            Map<String, Object> paramMap = new HashMap<>();
            PsiMethod currentMethod = (PsiMethod) psiElement;

            // 添加方法名
            paramMap.put(KEY_METHOD_NAME, currentMethod.getName());

            System.out.println("currentMethod.getName()=" + currentMethod.getName());

            // 添加注释
            PsiElement[] tags = currentMethod.getDocComment().getDescriptionElements();

            for (int i = 0; i < tags.length; i++) {
                System.out.println("tags[" + i + "].getText()=" + tags[i].getText());
                if (tags[i].getText() != null && !tags[i].getText().trim().equals("")){
                    // 添加注释
                    paramMap.put(KEY_METHOD_COMMENT_DATA, tags[i].getText().trim());
                    break;
                }
            }

            // 获取返回值
            PsiType returnType = currentMethod.getReturnType();
            String methodStr = returnType.toString();

            String returnStartStr = "BaseHttpResult<";
            int indexStartReturn = methodStr.indexOf(returnStartStr);
            int indexEndReturn = methodStr.lastIndexOf(">");

            String returnStr = methodStr.substring(indexStartReturn + returnStartStr.length(), indexEndReturn - 1);

            System.out.println("returnStr=" + returnStr);
            // 添加返回值
            paramMap.put(KEY_METHOD_RETURN, returnStr);


            PsiParameterList psiParameterList = currentMethod.getParameterList();

            StringBuilder params = new StringBuilder();
            StringBuilder paramsWithoutType = new StringBuilder();

            List<Pair> paramList = new ArrayList<Pair>();
            if(psiParameterList.getParametersCount() > 0){
                for (int i = 0; i < psiParameterList.getParametersCount(); i++) {
                    PsiParameter parameter = psiParameterList.getParameters()[i];

                    // 参数名做为key，因为参数名不重复
                    // 参数值做为value
                    Pair keyValue = new Pair(parameter.getName(), parameter.getTypeElement().getFirstChild().getText());

                    paramList.add(keyValue);

                    params.append(parameter.getTypeElement().getFirstChild().getText());
                    params.append(" ");
                    params.append(parameter.getName());


                    paramsWithoutType.append(parameter.getName());
                    if(i != psiParameterList.getParametersCount() - 1){
                        params.append(", ");
                        paramsWithoutType.append(", ");
                    }

                    System.out.println(i + ": parameter.getName()=" + parameter.getName());
                    System.out.println(i + ": parameter.getType().toString()=" + parameter.getTypeElement().getFirstChild().getText());

                }
            }

            // 添加方法参数字符串
            paramMap.put(KEY_METHOD_PARAMS_WITH_TYPE, params.toString());
            paramMap.put(KEY_METHOD_PARAMS_WITHOUT_TYPE, paramsWithoutType.toString());
            // 添加方法参数
            paramMap.put(KEY_METHOD_PARAMS, paramList);

            return paramMap;

        }

        return null;
    }
}
