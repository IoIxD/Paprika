package net.ioixd.paprika;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class Bridge implements Listener {
    Lua lua;
    RegisteredListener registeredListener;

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
            lua.functionExecuteAll(hookName, j);
        } else {
            HandlerList handler = event.getHandlers();
            handler.unregister(registeredListener);
        }
    }

    public LuaValue fullCoerce(Object event) {
        if(event == null) {
            return LuaValue.NIL;
        }
        LuaValue start = CoerceJavaToLua.coerce(event);
        if(event.getClass().getName().startsWith("java") ||
                event.getClass().getModifiers() != Modifier.PUBLIC) {
            return start;
        }
        LuaTable metatable = new LuaTable();
        metatable.set("__index", new IndexInterceptor());
        start.setmetatable(metatable);
        return CoerceJavaToLua.coerce(event);
    }
    public static class IndexInterceptor extends TwoArgFunction {
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
    }
}
