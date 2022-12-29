package net.ioixd.paprika;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

import com.google.gson.*;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.*;

import io.github.classgraph.*;

public class Bridge {

    Gson gson;

    Bridge() {
        gson = new Gson();
    }

    public void callFunction(Class<?> cls, String funcName, String json, Plugin plugin) throws Exception {
        // parse the arguments from the json
        HashMap<String,Object> args = new HashMap<>();
        args = gson.fromJson(json, args.getClass());

        // get the values from the arguments.
        // we can't use a vector because Class[].toArray() gets turned into Object[]
        Object[] final_args = new Object[args.entrySet().size()];
        Class<?>[] final_arg_classes = new Class<?>[args.entrySet().size()];
        int i = 0;
        for(Map.Entry<String, Object> obj : args.entrySet()) {
            final_args[i] = obj.getValue();
            final_arg_classes[i] = obj.getValue().getClass();
            i++;
        }

        // get the relevant method now
        Method method = cls.getMethod(funcName, final_arg_classes);
        // and get its parent.
        Object obj = classFromName(cls.getName(), plugin);

        method.invoke(obj, final_args);
    }

    public void callFunction(String className, String funcName, String json, Plugin plugin) throws Exception {
        Object cls = classFromName(className,plugin);
        callFunction(cls.getClass(),funcName,json,plugin);
    }

    public static LuaTable objectToLuaTable(Object object) {
        LuaTable table = new LuaTable();
        // see what getters the object has
        Class<?> cls = object.getClass();
        Method[] methods = cls.getMethods();
        for(Method method : methods) {
            if(method.getName().startsWith("get")) {
                // get the value in question and add it to the table.

                // if it requires values to obtain, skip it for now.
                if(method.getParameters().length >= 1) {
                    continue;
                }

                method.setAccessible(true); // :TROLL:

                // convert the method name a to snake case field name
                String name = method.getName().replace("get","");
                Pattern.compile("([A-Z])").matcher(name).replaceAll("_$1");
                name = name.toLowerCase();

                // run the method to get the value and return it as a lua value
                Object value;
                try {
                    value = method.invoke(object);
                } catch(IllegalAccessException ex) {
                    // WHAT
                    ex.printStackTrace();
                    continue;
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                    continue;
                }

                table.set(name, objectToLuaValue(value));
            };
        }
        return table;
    }

    public static LuaValue objectToLuaValue(Object object) {
        System.out.println(object.getClass().getName());
        switch(object.getClass().getName()) {
            case "java.lang.Integer":
                return LuaInteger.valueOf((Integer)object);
            case "java.lang.Boolean":
                return LuaBoolean.valueOf((Boolean)object);
            case "java.lang.Double":
                return LuaNumber.valueOf((double)object);
            case "java.lang.String":
                return LuaString.valueOf((String)object);
            case "java.lang.Enum":
                return LuaString.valueOf((object).toString());
        }
        // if we're here, then it's an object that needs to be converted manually.
        return objectToLuaTable(object);
    }

    public Object classFromName(String className, Plugin plugin) {
        switch(className) {
            case "org.bukkit.plugin":
                return plugin;
            case "org.bukkit.configuration.file.FileConfiguration":
                return plugin.getConfig();
            case "org.bukkit.plugin.PluginDescriptionFile":
                return plugin.getDescription();
            case "org.bukkit.plugin.PluginLoader":
                return plugin.getPluginLoader();
            case "org.bukkit.plugin.Server":
                return plugin.getServer();
            case "org.bukkit.plugin.PluginLogger":
            case "com.destroystokyo.paper.utils.PaperPluginLogger":
                return plugin.getLogger();
        }
        return null;
    }

    public Object[] getEvents() {
        ClassInfoList events = new ClassGraph()
                .enableClassInfo()
                .scan()
                .getClassInfo(Event.class.getName())
                .getSubclasses()
                .filter(info -> !info.isAbstract());

        Vector<Class<?>> theEvents = new Vector<>();

        try {
            for (ClassInfo event : events) {
                //noinspection unchecked
                Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(event.getName());

                if (Arrays.stream(eventClass.getDeclaredMethods()).anyMatch(method ->
                        method.getParameterCount() == 0 && method.getName().equals("getHandlers"))) {
                    theEvents.add(event.getClass());
                    //We could do this further filtering on the ClassInfoList instance instead,
                    //but that would mean that we have to enable method info scanning.
                    //I believe the overhead of initializing ~20 more classes
                    //is better than that alternative.
                }
            }
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Scanned class wasn't found", e);
        }

        return events.toArray();
    }
}
