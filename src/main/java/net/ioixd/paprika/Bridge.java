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
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import javax.script.Compilable;
import javax.script.CompiledScript;

public class Bridge {

    Gson gson = new Gson();
    Vector<Object> objectBuffer = new Vector<>();


    Bridge() {}

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

    public LuaValue objectToLuaTable(Object object) {
        return CoerceJavaToLua.coerce(object);
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
}
