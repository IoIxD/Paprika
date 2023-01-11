package net.ioixd.paprika;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CamelToSnakeCase {
    static Pattern snakeCase = Pattern.compile("(_)([A-Za-z])");
    static Pattern camelCase = Pattern.compile("([A-Z])");
    public static String convertToCamel(String str) {
        Matcher m = snakeCase.matcher(str);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while(m.find()) {
            sb.append(str, last, m.start());
            sb.append(m.group(0).replace("_","").toUpperCase());
            last = m.end();
        }
        sb.append(str.substring(last));
        return sb.substring(0,1).toUpperCase()+sb.substring(1);
    }
    public static String convertToSnake(String str) {
        Matcher m = camelCase.matcher(str);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while(m.find()) {
            sb.append(str, last, m.start());
            sb.append("_");
            sb.append(m.group(0).toLowerCase());
            last = m.end();
        }
        sb.append(str.substring(last));
        return sb.substring(1); // ignore the first character because its gonna have an underscore in front of it.
    }
}
