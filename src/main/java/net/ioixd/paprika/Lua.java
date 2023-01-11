package net.ioixd.paprika;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import java.io.*;
import javax.script.*;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.script.LuaScriptEngineFactory;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.luaj.vm2.script.LuajContext;

import java.lang.reflect.Field;
import java.util.*;

public class Lua {
    Paprika paprika;

    StringWriter sw;

    CompiledScript script;
    Bindings sb;
    ScriptEngine e;

    Bridge bridge;

    CommandMap commandMap;

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
        this.commandMap = (CommandMap) bukkitCommandMap.get(this.paprika.getServer());

        org.luaj.vm2.luajc.LuaJC.install(JsePlatform.standardGlobals());

        this.e = new LuaScriptEngineFactory().getScriptEngine();
        e.getContext().setWriter(this.sw);

        // set the package path
        String apath = pluginFolder.getAbsolutePath();
        char sep = File.separatorChar;
        StringBuilder path = new StringBuilder();

        // setup enums and static bindings
        setupEnumerators();

        this.paprika.getLogger().info("Resolving package path");
        path.append("package.path = \"")
                .append(apath)
                .append(sep)
                .append(";")
                .append(apath)
                .append(sep)
                .append("?.lua;")
                .append(findInitFilesInFolder(pluginFolder))
                .append("\"");

        //path.append(luaPackagePathGen(pluginFolder));
        this.paprika.getLogger().info(path.toString());
        script = ((Compilable) e).compile(path.toString());
        script.eval(this.sb);
        script.eval(e.getContext());

        this.paprika.getLogger().info("Compiling files");
        evalLuaFilesInFolder(pluginFolder,e);

        if(print) {
            // any prints that the files did should be printed
            this.broadcastBuffer();
        }

        // register lua hooks
        this.bridge = new Bridge(this.paprika);
    }

    public String findInitFilesInFolder(File folder) {
        if(folder.listFiles() == null) {
            return "";
        }
        StringBuilder path = new StringBuilder();
        File[] files = folder.listFiles();
        assert files != null;
        for(File file : files) {
            if(file.getName().endsWith("init.lua")) {
                path.append(file.getAbsolutePath().replace("init.lua","?.lua"))
                    .append(";");
            }
            if(file.listFiles() != null) {
                path.append(findInitFilesInFolder(file));
            }
        }
        return path.toString();
    }
    public void evalLuaFilesInFolder(File folder, ScriptEngine e) throws Exception {
        if(folder.listFiles() == null) {
            return;
        }
        File[] files = folder.listFiles();
        assert files != null;
        for(File file : files) {
            if(file.getName().endsWith(".lua")) {
                this.paprika.getLogger().info("Compiling "+file.getPath());
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
                        this.commandMap.register("paprika", new CustomCommand(funcName, this));
                    }
                    buffer.append(line).append("\n");
                }
                String code = buffer.toString();
                this.script = ((Compilable) e).compile(code);
                try {
                    this.script.eval(this.sb);
                    this.paprika.getLogger().info("Compiled "+file.getPath());
                } catch(LuaError ex) {
                    this.paprika.getLogger().warning("Skipping "+file.getPath()+"; \n"+ex.getMessage());
                }
            }
            if(file.listFiles() != null) {
                evalLuaFilesInFolder(file, e);
            }
        }
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

    // setup enums and static classes
    public void setupEnumerators() throws IOException {
        LuajContext context = (LuajContext) e.getContext();
        ClassPath classpath = ClassPath.from(Thread.currentThread().getContextClassLoader());
        for (ClassInfo classInfo : classpath.getAllClasses()) {
            String pkgName = classInfo.getPackageName();
            // only load things from the four supported classes.
            if(!(
                    pkgName.contains("minecraft") ||
                    pkgName.contains("spigot") ||
                    pkgName.contains("bukkit") ||
                    pkgName.contains("paper")
            )) {
                continue;
            }
            String name = classInfo.getSimpleName();
            // ignore "blank names"; private classes?
            if(name.equals("")) {
                continue;
            }
            Class<?> cls = classInfo.load();
            // so we need to bind these to some ugly looking globals first.
            // construct the ugly name.
            String uglyName = pkgName.replace(".","_")+"_"+CamelToSnakeCase.convertToSnake(name);
            System.out.println(uglyName);
            context.globals.set(uglyName, CoerceJavaToLua.coerce(cls));
        }
        e.setContext(context);
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