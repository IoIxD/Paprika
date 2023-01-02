package net.ioixd.paprika;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
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

import java.lang.reflect.Field;
import java.util.*;

public class Lua {
    Paprika paprika;

    HashMap<String, LuaValue> functions = new HashMap<>();
    Globals globals = null;

    StringWriter sw;

    CompiledScript script;
    Bindings sb = new SimpleBindings();

    Lua(Paprika paprika) {
        try {
            load(paprika);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void load(Paprika paprika) throws Exception {
        this.paprika = paprika;
        this.functions = new HashMap<>();

        // Start the Lua interpreter
        this.globals = JsePlatform.standardGlobals();

        File pluginFolder = paprika.getDataFolder();
        pluginFolder.mkdir();

        if(this.sw != null) {
            this.sw.close();
        }

        this.sw = new StringWriter();

        StringBuilder buffer = new StringBuilder();

        final Field bukkitCommandMap = this.paprika.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);
        CommandMap commandMap = (CommandMap) bukkitCommandMap.get(this.paprika.getServer());

        for(File file : pluginFolder.listFiles()) {
            if(file.getName().endsWith(".lua")) {
                // for debugging, skip any files starting with .
                if(file.getName().startsWith(".")) {
                    continue;
                }
                // open the file and look for any functions
                Scanner lineReader;
                lineReader = new Scanner(file);
                while(lineReader.hasNextLine()) {
                    String line = lineReader.nextLine();
                    // look for functions starting with "minecraftCommand..."
                    if(line.startsWith("function MinecraftCommand")) {
                        String funcName = line
                                .replace("function MinecraftCommand","")
                                .replace("\n","")
                                .replaceAll("\\((.*?)\\)","")
                                .toLowerCase();
                        commandMap.register("paprika", new CustomCommand(funcName, this));
                    }

                    buffer.append(line+"\n");
                }
            }
        }

        ScriptEngine e = new LuaScriptEngineFactory().getScriptEngine();
        script = ((Compilable) e).compile(buffer.toString());
        script.eval(sb);
        e.getContext().setWriter(this.sw);

        // register lua hooks
        new Bridge(this.paprika, this);
    }

    public String reload() {
        try {
            load(this.paprika);
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
            paprika.getServer().broadcastMessage(msg);
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
                    paprika.getLogger().severe(ex.getMessage());
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
