package me.xiba.plugin.template;

public class ViewModelMethodTomplate {

    // 1 方法名  %1$s
    // 2 方法名首字母大写  %2$s
    // 3 返回值  %3$s
    // 4 带类型的参数  %4$s
    // 5 参数  %5$s
    public static String METHOD_TEMPLATE =
            "   // ——————————————————————— ↓↓↓↓ <editor-fold desc=\" method\"> ↓↓↓↓ ——————————————————————— //\n" +
            "    \n" +
            "    private LoadingObserver<%3$s> %1$sObserver;\n" +
            "\n" +
            "    /**\n" +
            "     * 初始化「」LoadingObserver\n" +
            "     */\n" +
            "    private void init%2$sObserver(){\n" +
            "        %1$sObserver = new LoadingObserver<%3$s>(new LoadingObserver.ObserverOnNextListener<%3$s>() {\n" +
            "            @Override\n" +
            "            public void observerOnNext(%3$s value) {\n" +
            "                %1$sSuccessEvent.setValue(value);\n" +
            "            }\n" +
            "        }, mShowLoading, null);\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * 「」成功\n" +
            "     *\n" +
            "     * @return\n" +
            "     */\n" +
            "    private final SingleLiveEvent<%3$s> %1$sSuccessEvent = new SingleLiveEvent<>();\n" +
            "    public SingleLiveEvent<%3$s> get%2$sSuccessEvent() {\n" +
            "        return %1$sSuccessEvent;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     *\n" +
            "     */\n" +
            "    public void %1$s(%4$s) {\n" +
            "        %1$sObserver.cancelRequest();\n" +
            "        mModel.%1$s(%5$s, %1$sObserver);\n" +
            "    }\n" +
            "    // ———————————————————————————————————————————— ↑↑↑↑ </editor-fold> ↑↑↑↑ ———————————————————————————————————————————— //";

    // 1 方法名  %1$s
    // 2 方法名首字母大写  %2$s
    // 3 返回值  %3$s
    // 初始化Observer方法的模板
    public static String INIT_OBSERVER_METHOD_TEMPLATE =
            "private void init%2$sObserver(){\n" +
            "        %1$sObserver = new LoadingObserver<%3$s>(new LoadingObserver.ObserverOnNextListener<%3$s>() {\n" +
            "            @Override\n" +
            "            public void observerOnNext(%3$s value) {\n" +
            "                %1$sSuccessEvent.setValue(value);\n" +
            "            }\n" +
            "        }, mShowLoading, null);\n" +
            "    }";

    // SuccessEvent变量模板
    // 1 方法名  %1$s
    // 2 返回值  %2$s
    public static String SUCCESS_EVENT_FIELD_TEMPLATE =
            "private final SingleLiveEvent<%2$s> %1$sSuccessEvent = new SingleLiveEvent<>();";

    // getSuccessEvent方法模板
    // 1 方法名  %1$s
    // 2 方法名首字母大写  %2$s
    // 3 返回值  %3$s
    public static String GET_SUCCESS_EVENT_METHOD_TEMPLATE =
            "public SingleLiveEvent<%3$s> get%2$sSuccessEvent() {\n" +
            "        return %1$sSuccessEvent;\n" +
            "    }";

    // 调用model方法模板
    // 1 方法名  %1$s
    // 2 带类型的参数  %2$s
    // 3 参数  %3$s
    public static String MODEL_METHOD_TEMPLATE =
            "public void %1$s(%2$s) {\n" +
            "        %1$sObserver.cancelRequest();\n" +
            "        mModel.%1$s(%3$s, %1$sObserver);\n" +
            "    }";

    public static String START_EDITOR_FOLD_COMMENT_TEMPLATE =
            "// ————————————————————————————————————— ↓↓↓↓ <editor-fold desc=\" method\"> ↓↓↓↓ ————————————————————————————————————— //\n";

    public static String END_EDITOR_FOLD_COMMENT_TEMPLATE =
            "// ———————————————————————————————————————————— ↑↑↑↑ </editor-fold> ↑↑↑↑ ———————————————————————————————————————————— //";
}
