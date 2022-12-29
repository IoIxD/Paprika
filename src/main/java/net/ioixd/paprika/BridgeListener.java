package net.ioixd.paprika;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import net.ioixd.paprika.Bridge;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

public class BridgeListener implements Listener {
    Lua lua;
    RegisteredListener registeredListener;

    BridgeListener(Plugin plugin, Lua lua) {
        this.lua = lua;
        registeredListener = new RegisteredListener(this, (listener, event, objects) -> onEvent(listener, event), EventPriority.NORMAL, plugin, false);
        // the HandlerList object doesn't expose its name through anything.
        // so we have to register them all.
        // but we can unregister an event when we detect its not linked to anything.
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.register(registeredListener);
        }
    }

    void onEvent(Listener listener, Event event, Object[] ...objects) {
        if(lua == null) {
            return;
        }
        // get the event name
        String name = event.getEventName();
        String hookName = "On"+name.replace("Event","");
        // check if the corresponding function actually exists, and if it doesn't, unregister
        // this listener.
        if(lua.functionExists(hookName)) {
            LuaValue[] args = new LuaValue[objects.length];
            System.out.println(args.length);
            for(int i = 0; i < args.length; i++) {
                args[i] = Bridge.objectToLuaValue(objects[i]);
            }
            lua.functionExecuteAll(hookName, args);
        } else {
            HandlerList handler = event.getHandlers();
            handler.unregister(registeredListener);
        }
    }

}