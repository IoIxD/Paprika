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
    nullable: bool

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
            i += 1
        s += "}, \"description\": \""
        s += self.description.replace("\"","'")
        s += "\","
        s += "\"nullable\": \""+str(self.nullable).lower()+"\""
        s += "}"
        return s

    def __init__(self):
        self.name = ""
        self.modifier = ""
        self.ty = ""
        self.args = {}
        self.description = ""
        self.nullable = None

    @classmethod
    def fromtag(cls, tag: Tag, description: str, modifier: str, type: str):
        s = cls()
        s.ty = type
        s.name = tag.find("a").text
        s.modifier = modifier
        s.args = {}
        s.nullable = None

        method_parts: Match[str] = re.search(r"(.*?)\((.*?)\)", tag.text.replace("\n",""))
        if method_parts is not None:
            s.name = method_parts.group(1)
            args = method_parts.group(2).split(",")

            for arg in args:
                parts = arg.replace("\xa0", " ").split(" ")
                if(len(parts) == 1):
                    continue
                s.args[parts[1]] = parts[0]

        s.description = description.text
        return s

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
            if method is None:
                raise Exception("No methods. Did you return the method in fromtag?")
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
    def add_item(self, title, item, path, parent, index):
        # item
        tag: Tag = item.find_all("a")[index]

        # scrape the url with information about the item
        item_url = spigot_url_from_tag(path, parent, tag)
        item_html = get_url(item_url)
        item_tree = BeautifulSoup(item_html, "html.parser")

        print("> "+item_url)

        match title:
            case "Class Hierarchy" | "Interface Hierarchy":
                method_table: Tag = item_tree.find(id="method-summary-table")

                if(method_table is None):
                    method_table = item_tree.find(id="nested-class-summary")
                    if(method_table is None):
                        print("Warning: There was no method or classes table. Did you get a 404?")
                        return

                methods = []
                nested_classes = []

                modifier_tags: ResultSet = method_table.find_all("div", {"class": "col-first"})
                method_tags: ResultSet = method_table.find_all("div", {"class": "col-second"})
                description_tags: ResultSet = method_table.find_all("div", {"class": "col-last"})
                section_tags: ResultSet = item_tree.find_all("section", {"class": "detail"})

                i = 0
                for tag in method_tags:
                    if(tag.text == "Method" and tag.text == "Interface"):
                        i += 1
                        continue

                    tag: Tag = tag.find("code")
                    if tag is None:
                        i += 1
                        continue

                    # modifier
                    modifier_tag = modifier_tags[i].find("code")

                    modifier_parts = modifier_tag.text.split(" ")
                    type: str = ""
                    modifier: str = ""

                    if(len(modifier_parts) == 1):
                        modifier = "public"
                        type = modifier_parts[0]
                    else:
                        modifier = modifier_parts[0]
                        type = modifier_parts[1]

                    print(type)
                    if type == "class":
                        c = JavaClass.fromtag(tag, description_tags[i])
                        print(c)
                        nested_classes.append(c)
                    else:
                        m = Method.fromtag(tag, description_tags[i], modifier, type)
                        # get the later part of the page that says if it's nullable
                        for s in section_tags:
                            if s.find("h3").text == m.name:
                                annotations = s.find("span",{"class": "annotations"})
                                if annotations is not None:
                                    if annotations.find("a").text == "@Nullable":
                                        m.nullable = True
                                    if annotations.find("a").text == "@NotNull":
                                        m.nullable = False

                        methods.append(m)

                    i += 1

                parent = re.sub(r"(\(.*?\)|<.*?>|\s|\n)", "", parent)

                match title:
                    case "Class Hierarchy":
                        javaclass = JavaClass()
                        javaclass.name = parent
                        javaclass.methods = methods
                        description = item_tree.find(id="class-description").find("div",{"class": "block"})
                        if description is not None:
                            javaclass.description = description.text
                        self.classes.append(javaclass)
                    case "Interface Hierarchy":
                        javainterface = JavaInterface()
                        javainterface.name = parent
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
                        i += 1
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
                        i += 1
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

    def __add__(self, doc2):
        if(type(doc2) != type(self)):
            raise Exception("Cannot concatenate %s to Javadoc" % type(doc2))
        doc = Javadoc()
        for c in (self.classes + doc2.classes):
            doc.classes.append(c)
        for i in (self.interfaces + doc2.interfaces):
            doc.interfaces.append(i)
        for e in (self.enumerators + doc2.enumerators):
            doc.enumerators.append(e)
        return doc

def spigot_url_from_tag(path: str, parent: str, tag: Tag) -> str:
    if tag is None:
        raise Exception("tag provided is NoneType")
    parent = parent.replace("."+tag.text, "")
    parent_path = parent.replace(".","/")
    parent_path = re.sub(r"(\(.*?\)|<.*?>|\s|\n)", "", parent_path)

    return path+tag["href"]

def parse_javadoc(url: str) -> Javadoc:
    html = get_url(url)
    tree = BeautifulSoup(html, "html.parser")
    path = url.replace("package-tree.html","")

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
                doc.add_item(title, item, path, parent, i)
                i += 1

    return doc

