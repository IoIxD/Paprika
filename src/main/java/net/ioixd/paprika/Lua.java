package net.ioixd.paprika;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lua {
    Plugin plugin;
    final String functionRegexString = "function (.*?)\\((.*?)\\)";
    final Pattern functionRegex = Pattern.compile(functionRegexString);

    HashMap<String, LuaValue> functions = new HashMap<>();
    Globals globals = null;

    PrintStream printStream;
    ByteArrayOutputStream baos;

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

        this.baos = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.baos, true, "utf-8");
        globals.STDOUT = this.printStream;

        for(File file : pluginFolder.listFiles()) {
            if(file.getName().endsWith(".lua")) {
                // open the file and look for any functions
                Scanner lineReader = null;
                lineReader = new Scanner(file);
                StringBuffer buffer = new StringBuffer();
                String functionName = "";
                int i = 0;
                while(lineReader.hasNextLine()) {
                    // search for a line matching the function syntax.
                    String line = lineReader.nextLine();
                    Matcher matcher = functionRegex.matcher(line);
                    // begin loading a function if we find it.
                    if(matcher.find()) {
                        functionName = matcher.group(1);
                    }
                    buffer.append(line+"\n");
                    // finish loading it if we reach an end statement
                    if(line.startsWith("end")) {
                        if(functionName == "") {
                            this.plugin.getLogger().warning("Stray end detected in "+file.getName()+" at line "+i);
                        } else {
                            this.addFunction(functionName,buffer.toString());
                            buffer.setLength(0);
                        }
                    }
                    i++;
                }
                // if we have anything left over in the buffer, call it.
                if(!buffer.isEmpty()) {
                    globals.load(buffer.toString()).call();
                }
            }
        }

        // register lua hooks
        new BridgeListener(this.plugin, this);
    }

    public void addFunction(String functionName, String body) {
        LuaValue value = this.globals.load(body+"\n"+functionName+"()");
        // try to add the function to the map.
        // if it already exists, add an underscore to the name and try
        // again; this is useful for event handlers.
        boolean canAdd = false;
        while(!canAdd) {
            if(this.functions.containsKey(functionName)) {
                functionName = functionName+"_";
            } else {
                this.functions.put(functionName, value);
                canAdd = true;
            }
        }
        this.plugin.getLogger().info("Registered "+functionName);
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
        LuaValue func = this.functions.get(functionName);
        if(func == null) {
            throw new Exception("Function does not exist");
        }
        try {
            // apparently we can't call a function with more then three args, or a variable amount at that.
            switch(args.length) {
                case 0:
                    func.call();
                    break;
                case 1:
                    func.call(args[0]);
                    break;
                case 2:
                    func.call(args[2]);
                    break;
                case 3:
                    func.call(args[3]);
                    break;
                default:
                    throw new Exception("Cannot call a function with "+args.length+"args.");
            };
        } catch(LuaError ex) {
            this.plugin.getLogger().severe(ex.getMessage());
            return;
        }
        String content = this.baos.toString();
        if(this.baos.toByteArray().length >= 1) {
            plugin.getServer().broadcastMessage(content);
        }
        this.baos.reset();
    }

    // execute ALL functions with the given name
    public void functionExecuteAll(String functionName, LuaValue ...args) {
        boolean execute = true;
        while(execute) {
            if(functionExists(functionName)) {
                try {
                    functionExecute(functionName, args);
                    functionName += "_";
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                execute = false;
            }
        }

    }

    // check if a function exists
    public boolean functionExists(String functionName) {
        LuaValue func = this.functions.get(functionName);
        if(func == null) {
            return false;
        }
        return true;
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
