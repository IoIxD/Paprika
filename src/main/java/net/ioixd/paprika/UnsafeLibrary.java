package net.ioixd.paprika;

import java.lang.reflect.*;
import com.google.code.gson;

public class UnsafeLibrary {
    public void callFunction(String className, String funcName, String json) throws Exception {
        Object cl = Class.forName(className).getDeclaredConstructor(null).newInstance();
        Method method = cl.getClass().getDeclaredMethod(funcName);
        Parameter[] param = method.getParameters();
        for (Parameter p : param) {
            Class paramClass = p.getParameterizedType().getClass();
            Object paramObject = paramClass.getDeclaredConstructor(null).newInstance();
        }
    }

}
