from os.path import exists
import gen_json

# If there's no unsafe_lib.json, create it. Otherwise, load it.
unsafe_lib = ""
if(exists("./unsafe_lib.json")):
    print("unsafe_lib.json found, opening")
    f = open("./unsafe_lib.json")
    unsafe_lib = f.read()
    f.close()
else:
    print("unsafe_lib.json not found, creating")
    doc = gen_json.parse_javadoc("https://hub.spigotmc.org/javadocs/spigot/org/bukkit/package-tree.html")
    unsafe_lib = doc.toJSON().replace("\n","",99999999)
    f = open("unsafe_lib.json", "w")
    f.write()
    f.close()
print(unsafe_lib)