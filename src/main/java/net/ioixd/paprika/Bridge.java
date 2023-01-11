package net.ioixd.paprika;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

public class Bridge implements Listener {
    Lua lua;
    RegisteredListener registeredListener;

    Pattern upperCase = Pattern.compile("[A-Z]");

    Bridge(Paprika paprika) {
        this.lua = paprika.lua;
        this.registeredListener = new RegisteredListener(this, this::onEvent, EventPriority.NORMAL, paprika, false);
        // the HandlerList object doesn't expose its name through anything.
        // so we have to register them all.
        // but we can unregister an event when we detect it's not linked to anything.
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.unregister(paprika);
        }
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.register(this.registeredListener);
        }
    }

    public void onEvent(Listener listener, Event event) {
        if(lua == null) {
            return;
        }
        // get the event name
        String name = event.getEventName();
        String hookName = "On"+name.replace("Event","");
        // check if the corresponding function actually exists, and if it doesn't, unregister
        // this listener.
        if(lua.functionExists(hookName)) {
            LuaValue j = fullCoerce(event);
            lua.functionExecute(hookName, j);
        } else {
            HandlerList handler = event.getHandlers();
            handler.unregister(registeredListener);
        }
    }

    public static LuaValue fullCoerce(Object event) {
        if(event == null) {
            return LuaValue.NIL;
        }
        LuaValue start = CoerceJavaToLua.coerce(event);
        if(event.getClass().getName().startsWith("java") ||
                event.getClass().getModifiers() != Modifier.PUBLIC) {
            return start;
        }
        LuaValue val = CoerceJavaToLua.coerce(event);
        addHandlers(event, val);
        return val;
    }

    public static void addHandlers(Object obj, LuaValue val) {
        if(!val.istable() && !val.isuserdata()) {
            return;
        }
        LuaValue mt = val.getmetatable();
        if(mt == null) {
            mt = new LuaTable();
        }
        mt.set("__index", new LuaSyntaxToJavaSyntax.Index());
        mt.set("__newindex", new LuaSyntaxToJavaSyntax.NewIndex());
        mt.set("__len", new LuaSyntaxToJavaSyntax.Length(obj));
        mt.set("__name", LuaString.valueOf(obj.getClass().getSimpleName()));
        // does it have a constructor function?
        try {
            final Constructor<?> constructor = obj.getClass().getConstructor();
            mt.set("new", new LuaSyntaxToJavaSyntax.New(constructor));
        } catch (NoSuchMethodException ignored) {} // guess it doesn't.
        //mt.set("__pairs", new LuaSyntaxToJavaSyntax.Pairs(obj)); - LuaJ doesn't support this yet.
        val.setmetatable(mt);
    }
}
