package net.ioixd.paprika;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.*;

import javax.script.*;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.script.LuaScriptEngine;
import org.luaj.vm2.script.LuaScriptEngineFactory;
import org.luaj.vm2.script.LuajContext;

import java.lang.reflect.Field;
import java.util.*;

public class Lua {
    Paprika paprika;

    StringWriter sw;

    CompiledScript script;
    Bindings sb;

    Bridge bridge;

    Lua(Paprika paprika) {
        try {
            load(paprika, true);
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
                load(paprika, false);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    public void load(Paprika paprika, Boolean print) throws Exception {
        this.paprika = paprika;
        this.sb = new SimpleBindings();

        File pluginFolder = paprika.getDataFolder();
        pluginFolder.mkdir();

        this.sw = new StringWriter();

        final Field bukkitCommandMap = this.paprika.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);
        CommandMap commandMap = (CommandMap) bukkitCommandMap.get(this.paprika.getServer());

        ScriptEngine e = new LuaScriptEngineFactory().getScriptEngine();
        e.getContext().setWriter(this.sw);

        // set the package path
        String path = "package.path = \""+pluginFolder.getAbsolutePath()+File.separatorChar+"?.lua\"";
        this.paprika.getLogger().info(path);
        script = ((Compilable) e).compile(path);
        script.eval(this.sb);
        script.eval(e.getContext());

        for(File file : Objects.requireNonNull(pluginFolder.listFiles())) {
            if(file.getName().endsWith(".lua")) {
                StringBuilder buffer = new StringBuilder();
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
                                .replaceAll("\\((.*?)\\)","");
                        commandMap.register("paprika", new CustomCommand(funcName, this));
                    }
                    buffer.append(line).append("\n");
                }
                script = ((Compilable) e).compile(buffer.toString());
                script.eval(this.sb);
            }
        }

        if(print) {
            // any prints that the files did should be printed
            this.broadcastBuffer();
        }

        // register lua hooks
        this.bridge = new Bridge(this.paprika);
    }

    public String reload() {
        try {
            load(this.paprika, true);
        } catch(Exception ex) {
            return ChatColor.RED+ex.getMessage();
        }
        return "Reloaded files.";
    }
    public void broadcastBuffer() {
        if(this.sw.toString().length() >= 1) {
            String msg = this.sw.toString();
            if(msg.endsWith("\n")) {
                msg = msg.substring(0,msg.length()-1);
            }
            paprika.getServer().broadcastMessage(msg);
            this.sw.getBuffer().setLength(0);
        }
    }

    // execute a function
    public void functionExecute(String functionName, LuaValue ...args) {
        LuaFunction func = (LuaFunction) sb.get(functionName);
        func.invoke(args);
        this.broadcastBuffer();
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

        return "todo";
    }

    public String listCustomFunctions() {

        return "todo";
    }
}

/*
        metatable.set("__index", new LuaFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                String funcName = key.toString().substring(0,1).toUpperCase() + key.toString().substring(1);
                LuaValue func = table.get(funcName);
                System.out.println(funcName);
                if(func == null) {
                    return LuaValue.NIL;
                } else {
                    return func.call();
                }
            }
        });
 */