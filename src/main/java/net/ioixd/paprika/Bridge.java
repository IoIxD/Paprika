package net.ioixd.paprika;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public class Bridge implements Listener {
    Lua lua;
    RegisteredListener registeredListener;

    Bridge(Plugin plugin, Lua lua) {
        this.lua = lua;
        registeredListener = new RegisteredListener(this, this::onEvent, EventPriority.NORMAL, plugin, false);
        // the HandlerList object doesn't expose its name through anything.
        // so we have to register them all.
        // but we can unregister an event when we detect it's not linked to anything.
        for (HandlerList handler : HandlerList.getHandlerLists()) {
            handler.register(registeredListener);
        }
    }

    void onEvent(Listener listener, Event event) {
        if(lua == null) {
            return;
        }
        // get the event name
        String name = event.getEventName();
        String hookName = "On"+name.replace("Event","");
        // check if the corresponding function actually exists, and if it doesn't, unregister
        // this listener.
        if(lua.functionExists(hookName)) {
            lua.functionExecuteAll(hookName);
        } else {
            HandlerList handler = event.getHandlers();
            handler.unregister(registeredListener);
        }
    }

}
