package net.ioixd.paprika;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.*;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.script.LuaScriptEngineFactory;

import java.util.*;

public class Lua {
    Plugin plugin;

    HashMap<String, LuaValue> functions = new HashMap<>();
    Globals globals = null;

    StringWriter sw;

    CompiledScript script;
    Bindings sb = new SimpleBindings();

    Lua(Plugin plugin) {
        try {
            load(plugin);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        // wait one second and then reload it.
        // on cold boots, event listeners aren't registered
        // the first time, and we have to wait a bit and then
        // try again.
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                load(plugin);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void load(Plugin plugin) throws Exception {
        this.plugin = plugin;
        this.functions = new HashMap<>();

        // Start the Lua interpreter
        this.globals = JsePlatform.standardGlobals();

        File pluginFolder = plugin.getDataFolder();
        pluginFolder.mkdir();

        if(this.sw != null) {
            this.sw.close();
        }

        this.sw = new StringWriter();

        StringBuilder buffer = new StringBuilder();

        for(File file : pluginFolder.listFiles()) {
            if(file.getName().endsWith(".lua")) {
                // for debugging, skip any files starting with .
                if(file.getName().startsWith(".")) {
                    continue;
                }
                // open the file and look for any functions
                Scanner lineReader = null;
                lineReader = new Scanner(file);
                while(lineReader.hasNextLine()) {
                    String line = lineReader.nextLine();
                    buffer.append(line+"\n");
                }
            }
        }

        ScriptEngine e = new LuaScriptEngineFactory().getScriptEngine();
        script = ((Compilable) e).compile(buffer.toString());
        script.eval(sb);
        e.getContext().setWriter(this.sw);

        // register lua hooks
        new Bridge(this.plugin, this);
    }

    public String reload() {
        try {
            load(this.plugin);
        } catch(Exception ex) {
            return ChatColor.RED+ex.getMessage();
        }
        return "Reloaded files.";
    }

    // execute a function
    public void functionExecute(String functionName, LuaValue ...args) throws Exception {
        LuaFunction func = (LuaFunction) sb.get(functionName);
        switch (args.length) {
            case 0 -> func.call();
            case 1 -> func.call(args[0]);
            case 2 -> func.call(args[0], args[1]);
            case 3 -> func.call(args[0], args[1], args[2]);
            default -> throw new Exception("Too many args, up to three allowed");
        }
        if(this.sw.toString().length() >= 1) {
            String msg = this.sw.toString();
            if(msg.endsWith("\n")) {
                msg = msg.substring(0,msg.length()-1);
            }
            plugin.getServer().broadcastMessage(msg);
            this.sw.getBuffer().setLength(0);
        }
    }

    // execute ALL functions with the given name
    public void functionExecuteAll(String functionName, LuaValue ...args) {
        boolean execute = true;
        while(execute) {
            if(functionExists(functionName)) {
                try {
                    functionExecute(functionName, args);
                    functionName += "_";
                } catch(LuaError ex) {
                    plugin.getLogger().severe(ex.getMessage());
                    execute = false;
                } catch(Exception ex) {
                    ex.printStackTrace();
                    execute = false;
                }
            } else {
                execute = false;
            }
        }

    }

    // check if a function exists
    public boolean functionExists(String functionName) {
        LuaFunction func = (LuaFunction) sb.get(functionName);
        return func != null;
    }

    public String listMinecraftFunctions() {
        return "none yet";
    }

    public String listCustomFunctions() {
        String str = "";
        for(Map.Entry<String, LuaValue> obj : this.functions.entrySet()) {
            str += "- "+obj.getKey()+"\n";
        }
        return str;
    }

    public void printHelp() {

    }
}
