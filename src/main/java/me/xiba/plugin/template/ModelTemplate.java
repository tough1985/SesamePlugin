package me.xiba.plugin.template;

public class ModelTemplate {
    //第一个参数是包路径
    //第二个参数是类名
    //第三个参数是对应的方法体
    public static String MODEL_TEMPLATE =
            "package %1$s.model;\n" +
                    "\n" +
                    "\n" +
                    "import com.fangdd.mobile.mvvmcomponent.model.BaseHttpModel;\n" +
                    "import com.fangdd.mobile.realtor.common.http.LoadingObserver;\n" +
                    "import %1$s.service.%2$sService;\n" +
                    "\n" +
                    "import io.reactivex.Observable;\n" +
                    "\n" +
                    "/**\n" +
                    " * @Author:\n" +
                    " * @Date:\n" +
                    " * @Description:\n" +
                    " */\n" +
                    "public class %2$sModel extends BaseHttpModel {\n" +
                    "\n" +
                    "    public %2$sService getService() {\n" +
                    "    }\n" +
                    "\n" +
                    "%3$s" +
                    "\n" +
                    "}\n";
}
