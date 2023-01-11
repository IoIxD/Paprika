package net.ioixd.paprika;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Vector;

public class LuaSyntaxToJavaSyntax {

    // __index metamethod for mapping getters
    public static class Index extends TwoArgFunction {
        HashMap<LuaValue, LuaValue> cache = new HashMap<>();

        @Override
        public LuaValue call(LuaValue table, LuaValue key) {
            if(table == NIL) {
                return NIL;
            }

            if(cache.get(key) != null) {
                return cache.get(key);
            }

            // prevent stack overflows
            if(key.toString().toLowerCase().startsWith("getget") || key.toString().toLowerCase().startsWith("isis")) {
                return NIL;
            }

            // convert the value we got to camel case.
            String newVal = CamelToSnakeCase.convertToCamel(key.toString());

            // return the result of a call to a getter method; nil if not existent.
            LuaValue val = table.method("get"+newVal);
            if(val == LuaValue.NIL) {
                val = table.method("is"+newVal);
            }
            Bridge.addHandlers(table, val);
            LuaValue v;
            if(val.isfunction()) {
                v = val.call();
            } else {
                v = val;
            }
            this.cache.put(key, v);
            return v;
        }
    }

    // __newindex for mapping setters
    public static class NewIndex extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
            if(table == NIL) {
                return NIL;
            }

            // convert the value we got to camel case.
            String newVal = CamelToSnakeCase.convertToCamel(key.toString());

            String funcName = "set"+newVal;
            return table.method(funcName, value);
        }
    }

    // __len metamethod for getting the length of something
    public static class Length extends ZeroArgFunction {
        Object ogObject;

        LuaValue cachedLength = LuaValue.valueOf(-1);

        Length(Object object) {
            this.ogObject = object;
        }
        @Override
        public LuaValue call() {
            if(cachedLength != LuaValue.valueOf(-1)) {
                return cachedLength;
            }
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
                LuaValue val = CoerceJavaToLua.coerce(methodToCall.invoke(relObject));
                this.cachedLength = val;
                return val;
            } catch (Exception e) {
                throw new LuaError("Couldn't get length for "+relObject.getClass().getName()+": "+e.getMessage());
            }
        }
    }

    // __pairs - LuaJ actually doesn't support this and the PR that adds it is five years old
    // but it was reviewed two months ago so I'll keep it.
    // ...honestly i might just use the repo i found that has the rest of the 5.2 features.
    public static class Pairs extends OneArgFunction {
        Object object;
        LuaTable cached;

        Pairs(Object object) {
            this.object = object;
        }

        @Override
        public LuaValue call(LuaValue table) {
            if(cached != null) {
                return cached;
            }
            LuaTable newTable = new LuaTable();
            HashMap<?,?> map;
            try {
                map = (HashMap<?,?>)object;
            } catch (Exception e) {
                throw new LuaError("tried to index "+object.getClass().getSimpleName()+" as a hashmap");
            }
            for(java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                LuaValue key = CoerceJavaToLua.coerce(entry.getKey());
                LuaValue val = CoerceJavaToLua.coerce(entry.getValue());
                newTable.set(key, val);
            }
            cached = newTable;
            return newTable;
        }
    }

    public static class New extends ZeroArgFunction {
        Constructor<?> constructor;

        New(Constructor<?> constructor) {
            this.constructor = constructor;
        };

        @Override
        public LuaValue call() {
            try {
                return CoerceJavaToLua.coerce(constructor.newInstance());
            } catch (Exception e) {
                throw new LuaError(e.getMessage());
            }
        }
    }
    // __lpairs

    // __add
    // __sub?
    // __mul?
    // __div?
    // __unm (negation)
    // __pow (expoententian
    // __eq
    // __lt
    // __le

    // __concat
}
