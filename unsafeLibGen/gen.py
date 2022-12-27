# https://hub.spigotmc.org/javadocs/spigot/org/bukkit/package-tree.html

import requests, re, os
from re import Match
from bs4 import BeautifulSoup, ResultSet, Tag
from os.path import exists

def get_url(url) -> str:
    urle = url.replace("/","_",99).replace("\\","_",99)
    if exists("./.cache/"+urle):
        f = open("./.cache/"+urle, "r")
        body = f.read()
        f.close()
        return body
    else:
        body = requests.get(url).text
        if os.path.exists("./.cache") is False:
            os.mkdir("./.cache")
        f = open("./.cache/"+urle, "w")
        f.write(body)
        f.close()
        return body


class Method:
    name: str = ""
    modifier: str = ""
    ty: str = ""
    args = {}
    description: str = ""

    def toJSON(self):
        s: str = ""
        # yes i "RTFM", try to use json encode gives a weird error.
        # AttributeError: 'mappingproxy' object has no attribute '__dict__'. Did you mean: '__dir__'?
        s += "{\"name\": \""
        s += self.name
        s += "\", \"modifier\": \""
        s += self.modifier
        s += "\", \"ty\": \""
        s += self.ty
        s += "\", \"args\": {"
        i = 0
        for key, value in self.args.items():
            s += "\""+str(key)+"\": \""+str(value)+"\""
            if i != len(self.args.items())-1:
                s += ", "
        s += "}, \"description\": \""
        s += self.description.replace("\"","'")
        s += "\"}"
        return s

    def __init__(self):
        self.name = ""
        self.modifier = ""
        self.ty = ""
        self.args = {}
        self.description = ""

    def fromtag(tag: Tag, description: str, modifier: str, type: str):
        self = Method()
        self.ty = type
        self.name = tag.find("a").text
        self.modifier = modifier
        method_parts: Match[str] = re.search(r"(.*?)\((.*?)\)", tag.text)
        if method_parts is not None:
            self.name = method_parts.group(1)
            args = method_parts.group(2).split(",")

            for arg in args:
                parts = arg.replace("\xa0", " ").split(" ")
                if(len(parts) == 1):
                    continue
                self.args[parts[1]] = parts[0]

        self.description = description.text
        return self

class JavaClass:
    java_import: str  = ""
    name: str = ""
    methods: list[Method] = []
    description: str = ""

    def toJSON(self):
        s: str = ""
        # yes i "RTFM", try to use json encode gives a weird error.
        # AttributeError: 'mappingproxy' object has no attribute '__dict__'. Did you mean: '__dir__'?
        s += "{\"java_import\": \""
        s += self.java_import
        s += "\", \"name\": \""
        s += self.name
        s += "\", \"methods\": ["
        i = 0
        for method in self.methods:
            s += method.toJSON()
            if(i != len(self.methods)-1):
                s += ", "
            i += 1
        s += "], \"description\": \""
        s += self.description.replace("\"","'")
        s += "\"}"
        return s

    def __init__(self):
        self.java_import = ""
        self.name = ""
        self.methods = []
        self.description = ""

    def fromtag(tag: Tag, description: str):
        self = JavaClass()
        self.name = tag.find("a").text
        javaclass_parts: Match[str] = re.search(r"(.*?)\((.*?)\)", tag.text)
        if javaclass_parts is not None:
            self.name = javaclass_parts.group(1)
            args = javaclass_parts.group(2).split(",")
            for arg in args:
                parts = arg.replace("\xa0", " ").split(" ")
                if(len(parts) == 1):
                    continue
                self.args[parts[1]] = parts[0]

        self.description = description.text
        return self

class JavaInterface:
    java_import: str  = ""
    name: str = ""
    methods: list[Method] = []
    description: str = ""
    nested_classes: list[JavaClass] = []

    def __init__(self):
        self.java_import = ""
        self.name = ""
        self.methods = []
        self.description = ""
        self.nested_classes = []

    def toJSON(self):
        s: str = ""
        # yes i "RTFM", try to use json encode gives a weird error.
        # AttributeError: 'mappingproxy' object has no attribute '__dict__'. Did you mean: '__dir__'?
        s += "{\"java_import\": \""
        s += self.java_import
        s += "\", \"name\": \""
        s += self.name
        s += "\", \"methods\": ["
        i = 0
        for method in self.methods:
            if method is not None:
                s += method.toJSON()
            if(i != len(self.methods)-1):
                s += ", "
            i += 1
        s += "], \"nested_classes\": ["
        i = 0
        for c in self.nested_classes:
            s += c.toJSON()
            if(i != len(self.nested_classes)-1):
                s += ", "
            i += 1
        s += "]}"
        return s

class JavaEnumConstant:
    name: str = ""
    description: str = ""

    def __init__(self):
        self.name = ""
        self.description = ""

    def toJSON(self):
        s: str = ""
        # yes i "RTFM", try to use json encode gives a weird error.
        # AttributeError: 'mappingproxy' object has no attribute '__dict__'. Did you mean: '__dir__'?
        s += "{\"name\": \""
        s += self.name
        s += "\", \"description\": \""
        s += self.description.replace("\"","'")
        s += "\"}"
        return s

class JavaEnum:
    java_import: str = ""
    name: str = ""
    methods: list[Method] = []
    constants: list[JavaEnumConstant] = []

    def __init__(self):
        self.java_import = ""
        self.name = ""
        self.methods = []
        self.description = ""

    def toJSON(self):
        s: str = ""
        # yes i "RTFM", try to use json encode gives a weird error.
        # AttributeError: 'mappingproxy' object has no attribute '__dict__'. Did you mean: '__dir__'?
        s += "{\"java_import\": \""
        s += self.java_import
        s += "\", \"name\": \""
        s += self.name
        s += "\", \"methods\": ["
        i = 0
        for method in self.methods:
            s += method.toJSON()
            if(i != len(self.methods)-1):
                s += ", "
            i += 1
        s += "], \"nested_classes\": ["
        for c in self.constants:
            s += c.toJSON()
            if(i != len(self.constants)-1):
                s += ", "
            i += 1
        s += "]}"
        return s

class Javadoc:
    classes: list[JavaClass] = []
    interfaces: list[JavaClass] = []
    enumerators: list[JavaEnum] = []

    @classmethod
    def add_item(self, title, item, parent, index):
        # item
        tag: Tag = item.find_all("a")[index]

        # scrape the url with information about the item
        item_url = spigot_url_from_tag(parent, tag)
        item_html = get_url(item_url)
        item_tree = BeautifulSoup(item_html, "html.parser")

        print("> "+item_url)

        match title:
            case "Class Hierarchy" | "Interface Hierarchy":
                method_table: Tag = item_tree.find(id="method-summary-table")

                if(method_table is None):
                    method_table = item_tree.find(id="nested-class-summary")
                    if(method_table is None):
                        raise Exception("There was no method or classes table. Did you get a 404?")

                methods = []
                nested_classes = []

                modifier_tags: ResultSet = method_table.find_all("div", {"class": "col-first"})
                method_tags: ResultSet = method_table.find_all("div", {"class": "col-second"})
                description_tags: ResultSet = method_table.find_all("div", {"class": "col-last"})

                i = 0
                for tag in method_tags:
                    if(tag.text == "Method" and tag.text == "Interface"):
                        i += 1
                        continue

                    tag: Tag = tag.find("code")
                    if tag is None:
                        continue

                    # modifier
                    modifier_tag = modifier_tags[i]

                    if(modifier_tag.text == "Modifier and Type"):
                        i += 1
                        modifier_tag = modifier_tags[i]

                    modifier_parts = modifier_tag.text.split(" ")
                    type: str = ""
                    modifier: str = ""
                    if(len(modifier_parts) == 1):
                        modifier = "package-private"
                        type = modifier_tag.name
                    else:
                        modifier = modifier_parts[0]
                        type = modifier_parts[1]

                    if type == "class":
                        nested_classes.append(JavaClass.fromtag(tag, description_tags[i]))
                    else:
                        methods.append(Method.fromtag(tag, description_tags[i], modifier, type))

                    i += 1

                match title:
                    case "Class Hierarchy":
                        javaclass = JavaClass()
                        javaclass.java_import = parent
                        javaclass.name = tag.text
                        javaclass.methods = methods
                        description = item_tree.find(id="class-description").find("div",{"class": "block"})
                        if description is not None:
                            javaclass.description = description.text
                        self.classes.append(javaclass)
                    case "Interface Hierarchy":
                        javainterface = JavaInterface()
                        javainterface.java_import = parent
                        javainterface.name = tag.text
                        javainterface.methods = methods
                        javainterface.nested_classes = nested_classes
                        description = item_tree.find(id="class-description").find("div",{"class": "block"})
                        if description is not None:
                            javainterface.description = description.text
                        self.interfaces.append(javainterface)

            # we don't care about the annotation hierarchy

            case "Enum Hierarchy":
                enum_table: Tag = item_tree.find(id="enum-constant-summary")
                enum_tags: ResultSet = enum_table.find_all("div", {"class": "col-first"})
                description_tags: ResultSet = enum_table.find_all("div", {"class": "col-last"})

                i = 0

                enums = JavaEnum()
                enums.java_import = parent
                enums.name = title.replace("Enum ","")

                # get the enum names
                for tag in enum_tags:
                    if(tag.text == "Enum Constant"):
                        continue
                    enum = JavaEnumConstant()
                    enum.name = tag.find("code").find("a").text
                    enum.description = description_tags[i].text
                    enums.constants.append(enum)
                    i += 1

                # get the methods
                method_table: Tag = item_tree.find(id="method-summary-table")

                if(method_table is None):
                    raise Exception("There was no method or classes table. Did you get a 404?")

                modifier_tags: ResultSet = method_table.find("div", {"class": "col-first"})
                method_tags: ResultSet = method_table.find("div", {"class": "col-second"})
                description_tags: ResultSet = method_table.find("div", {"class": "col-last"})

                methods = []

                i = 0
                for tag in method_tags:
                    if(tag.text == "Method"):
                        i += 1
                        continue

                    tag: Tag = tag.find("code")
                    if tag is None:
                        continue

                    # modifier
                    modifier_tag = modifier_tags[i]

                    if(modifier_tag.text == "Modifier and Type"):
                        i += 1
                        modifier_tag = modifier_tags[i]

                    modifier_parts = modifier_tag.text.split(" ")
                    type: str = ""
                    modifier: str = ""
                    if(len(modifier_parts) == 1):
                        modifier = "package-private"
                        type = modifier_tag.name
                    else:
                        modifier = modifier_parts[0]
                        type = modifier_parts[1]

                    methods.append(Method.fromtag(tag, description_tags[i], modifier, type))

                    i += 1
                enums.methods = methods

                self.enumerators.append(enums)

    def toJSON(self):
        s: str = ""
        s += "{\"classes\": ["
        i = 0
        for c in self.classes:
            s += c.toJSON()
            if(i != len(self.classes)-1):
                s += ", "
            i += 1
        s += "]"
        s += ",\"interfaces\": ["
        i = 0
        for c in self.interfaces:
            s += c.toJSON()
            if(i != len(self.interfaces)-1):
                s += ", "
            i += 1
        s += "]"
        s += ",\"enumerators\": ["
        i = 0
        for c in self.enumerators:
            s += c.toJSON()
            if(i != len(self.enumerators)-1):
                s += ", "
            i += 1
        s += "]}"
        return s

def spigot_url_from_tag(parent: str, tag: Tag) -> str:
    if tag is None:
        raise Exception("tag provided is NoneType")
    parent = parent.replace("."+tag.text, "")
    parent_path = parent.replace(".","/")
    parent_path = re.sub(r"(\(.*?\)|<.*?>|\s|\n)", "", parent_path)

    return "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/"+tag["href"]

def parse_javadoc(url: str) -> Javadoc:
    html = get_url(url)
    tree = BeautifulSoup(html, "html.parser")
    # validate it
    h2 = tree.find_all("h2", {"title": "Class Hierarchy"})
    if(len(h2) <= 0):
        raise Exception("Invalid page given; html does not have a header named 'Class Hierarchy'")
    # ok get all the lists and put them into the class
    doc = Javadoc()
    sections = tree.find_all("section", {"class": "hierarchy"})
    for section in sections:
        # get the title of the section
        section_header: Tag = section.find('h2')
        title = section_header.get("title")
        ul: Tag = section.find("ul")
        items: list[Tag] = ul.find_all("li")
        for item in items:
            # parent class
            parent: str = item.text
            if(parent.startswith("java")):
                continue

            parent_parts = parent.split("\n")
            i = 0
            for part in parent_parts:
                parent = parent_parts[i]
                if(parent == ""):
                    continue
                doc.add_item(title, item, parent, i)
                i += 1

    return doc

print("Parsing the javadoc into an object")
doc = parse_javadoc("https://hub.spigotmc.org/javadocs/spigot/org/bukkit/package-tree.html")
f = open("unsafe_objects.json", "w")
f.write(doc.toJSON().replace("\n","",99999999))
f.close()