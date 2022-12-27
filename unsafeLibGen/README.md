## unsafeLibGen

This is a Python script that goes through the JavaDoc for Spigot and auto-generates a class that lets one call the functions through strings and JSON. The actual Lua side then intercepts calls to functions prefaced with `unsafe_` to the functions here.