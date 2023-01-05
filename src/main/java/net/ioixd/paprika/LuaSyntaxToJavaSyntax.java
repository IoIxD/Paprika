package net.ioixd.paprika;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaSyntaxToJavaSyntax {

    // __index metamethod
    public static class Index extends TwoArgFunction {
        Pattern snakeCase = Pattern.compile("(_)([A-Za-z])");

        @Override
        public LuaValue call(LuaValue table, LuaValue key) {
            if(table == NIL) {
                return NIL;
            }

            // convert the value we got to camel case.
            String text = key.toString();
            Matcher m = snakeCase.matcher(text);
            StringBuilder sb = new StringBuilder();
            int last = 0;
            while(m.find()) {
                sb.append(text, last, m.start());
                sb.append(m.group(0).replace("_","").toUpperCase());
                last = m.end();
            }
            sb.append(text.substring(last));
            String newVal = sb.substring(0,1).toUpperCase()+sb.substring(1);

            // return the result of a call to a getter method; nil if not existent.
            return table.method("get"+newVal);
        }
    }
}
