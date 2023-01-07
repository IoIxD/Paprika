package net.ioixd.paprika;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaSyntaxToJavaSyntax {

    // __index metamethod for mapping getters
    public static class Index extends TwoArgFunction {
        Pattern snakeCase = Pattern.compile("(_)([A-Za-z])");

        @Override
        public LuaValue call(LuaValue table, LuaValue key) {
            if(table == NIL) {
                return NIL;
            }

            // prevent stack overflows
            if(key.toString().toLowerCase().startsWith("getget") || key.toString().toLowerCase().startsWith("isis")) {
                return NIL;
            }

            // convert the value we got to camel case.
            String text = key.toString();
            Matcher m = snakeCase.matcher(text);
            StringBuilder sb = new StringBuilder();
            int last = 0;
            while(m.find()) {
                sb.append(text, last, m.start());
                sb.append(m.group(0).replace("_","").toUpperCase());
                last = m.end();
            }
            sb.append(text.substring(last));
            String newVal = sb.substring(0,1).toUpperCase()+sb.substring(1);

            // return the result of a call to a getter method; nil if not existent.
            LuaValue val = table.method("get"+newVal);
            if(val == LuaValue.NIL) {
                val = table.method("is"+newVal);
            }
            Bridge.addHandlers(table, val);
            if(val.isfunction()) {
                return val.call();
            } else {
                return val;
            }
        }
    }

    // __newindex for mapping setters
    public static class NewIndex extends ThreeArgFunction {
        Pattern snakeCase = Pattern.compile("(_)([A-Za-z])");

        @Override
        public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
            if(table == NIL) {
                return NIL;
            }

            // convert the value we got to camel case.
            String text = key.toString();
            Matcher m = snakeCase.matcher(text);
            StringBuilder sb = new StringBuilder();
            int last = 0;
            while(m.find()) {
                sb.append(text, last, m.start());
                sb.append(m.group(0).replace("_","").toUpperCase());
                last = m.end();
            }
            sb.append(text.substring(last));
            String newVal = sb.substring(0,1).toUpperCase()+sb.substring(1);

            String funcName = "set"+newVal;
            return table.method(funcName, value);
        }
    }

    // __len metamethod for getting the length owf something
    public static class Length extends ZeroArgFunction {
        Object ogObject;
        Paprika paprika;

        Length(Object object) {
            this.ogObject = object;
        }
        @Override
        public LuaValue call() {
            Object relObject = this.ogObject;
            Class<?> cls = relObject.getClass();

            // there are many ways to get the length of something in java.
            Method methodToCall = null;
            try {
                methodToCall = cls.getMethod("length");
            } catch (NoSuchMethodException ignored) {}
            try {
                methodToCall = cls.getMethod("size");
            } catch (NoSuchMethodException ignored) {}
            // if it has a "values" method, resolve that and check THAT;

            // this is useful for java hashmaps.
            try {
                Method method = cls.getMethod("values");
                // if we make it here, try and resolve the method and find the methods there.
                try {
                    relObject = method.invoke(relObject);
                } catch (Exception e) {
                    throw new LuaError("Couldn't get values for "+relObject.getClass().getName()+": "+e.getMessage());
                }
                Class<?> cls2 = relObject.getClass();
                // it might just have the length field.
                try {
                    return CoerceJavaToLua.coerce(cls2.getField("length"));
                } catch (NoSuchFieldException ignored) {}
                try {
                    methodToCall = cls2.getMethod("length");
                } catch (NoSuchMethodException ignored) {}
                try {
                    methodToCall = cls2.getMethod("size");
                } catch (NoSuchMethodException ignored) {}
            } catch (NoSuchMethodException ignored) {}

            if(methodToCall == null) {
                return NIL;
            }
            // try and call the method we got.
            try {
                return CoerceJavaToLua.coerce(methodToCall.invoke(relObject));
            } catch (Exception e) {
                throw new LuaError("Couldn't get length for "+relObject.getClass().getName()+": "+e.getMessage());
            }
        }
    }

    // __concat

    // __pairs

    // __lpairs

    // __close?

    // __add?
    // __sub?
    // __mul?
    // __div?
    // __unm (negation)
    // __pow (expoententian
    // __eq
    // __lt
    // __le

}
