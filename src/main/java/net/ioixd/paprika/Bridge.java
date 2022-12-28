package net.ioixd.paprika;

import java.lang.reflect.*;
import java.util.*;
import com.google.gson.*;
import org.bukkit.plugin.Plugin;

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
        Object obj = forceInstantiate(cls, plugin);

        method.invoke(obj, final_args);
    }

    public void callFunction(String className, String funcName, String json, Plugin plugin) throws Exception {
        Object cls = classFromName(className,plugin);
        callFunction(cls.getClass(),funcName,json,plugin);
    }

    public Object forceInstantiate(Class<?> cls, Plugin plugin) throws Exception {
        // see if we can or have to get it via a global
        Object o = classFromName(cls.getName(),plugin);
        if(o != null) {
            return o;
        }
        // if not then try and construct it (and see what fun error we get!).

        Constructor<?>[] constructors = cls.getDeclaredConstructors();
        if(constructors.length == 0) {
            throw new Exception(cls.getName()+" has no constructors.");
        }
        Constructor<?> constructor = constructors[0];
        constructor.setAccessible(true);
        Vector<Object> args = new Vector<>();
        for(Parameter arg : constructor.getParameters()) {
            Class<?> ty = arg.getType();
            args.add(forceInstantiate(ty, plugin));
        }
        Object obj;
        try {
            obj = constructor.newInstance(args);
        } catch(IllegalArgumentException ex) {
            System.out.println(constructor);
            obj = constructor.newInstance();
        } catch(InstantiationException ex) {
            Class<?> lol = constructor.getClass().asSubclass(Object.class);
            if(lol.getTypeName() == "java.lang.reflect.Constructor") {
                // nope
                obj = cls;
            } else {
                obj = forceInstantiate(lol, plugin);
            }
        }
        return obj;
    };

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

}
