package net.ioixd.paprika;
import org.bukkit.plugin.Plugin;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ioixd.paprika.lua_functions.*;

public class Lua {
    Plugin plugin;
    String functionRegexString = "function (.*?)\\(\\)";
    Pattern functionRegex = Pattern.compile(functionRegexString);

    Vector<LuaValue> chunks = new Vector<>();
    HashMap<String, LuaValue> functionLinks = new HashMap<>();

    Globals globals = null;

    PrintStream printStream;
    ByteArrayOutputStream baos;

    Lua(Plugin plugin) {
        this.plugin = plugin;

        // Start the Lua interpreter
        this.globals = JsePlatform.standardGlobals();

        File pluginFolder = plugin.getDataFolder();
        pluginFolder.mkdir();

        this.baos = new ByteArrayOutputStream();
        try {
            this.printStream = new PrintStream(this.baos, true, "utf-8");
        } catch(Exception ex) {
            ex.printStackTrace();
            return;
        }
        globals.STDOUT = this.printStream;

        for(File file : pluginFolder.listFiles()) {
            if(file.getName().endsWith(".lua")) {
                // open the file and look for any functions with empty arguments
                Scanner lineReader = null;
                try {
                    lineReader = new Scanner(file);
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
                LuaValue c = this.globals.loadfile(file.getPath());
                while(lineReader.hasNextLine()) {
                    String line = lineReader.nextLine();
                    System.out.println(line);
                    Matcher matcher = functionRegex.matcher(line);
                    if(matcher.find()) {
                        String functionName = matcher.group(1);
                        this.plugin.getLogger().info("Registered new command: "+functionName);
                        this.functionLinks.put(functionName, c);
                    }
                }
                chunks.add(c);
            }
        }
    }

    public void functionExecute(String functionName) throws Exception {
        LuaValue func = this.functionLinks.get(functionName);
        if(func == null) {
            throw new Exception("Function does not exist");
        }
        func.call();
        String content = this.baos.toString(StandardCharsets.UTF_8);
        plugin.getServer().broadcastMessage(content);
        this.baos.reset();
    }

    public String listFunctions() {
        String str = "";
        for(Map.Entry<String, LuaValue> obj : this.functionLinks.entrySet()) {
            str += "- "+obj.getKey()+"\n";
        }
        return str;
    }
}
/*
  public void testPrintToStringFromLuaj() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(baos, true, "utf-8");
    Globals globals = JsePlatform.standardGlobals();
    globals.STDOUT = printStream;
    String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }
 */
