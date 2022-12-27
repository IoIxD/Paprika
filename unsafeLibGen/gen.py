# https://hub.spigotmc.org/javadocs/spigot/org/bukkit/package-tree.html

import requests, re
from re import Match
from bs4 import BeautifulSoup, ResultSet, Tag
import json

class Method:
    name: str
    modifier: str
    ty: str
    args = {}
    description: str
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__,
            sort_keys=True, indent=4)

class JavaClass:
    java_import: str  = ""
    name: str = ""
    methods: list[Method] = []
    description: str = ""
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__,
            sort_keys=True, indent=4)

class JavaInterface:
    java_import: str  = ""
    name: str = ""
    methods: list[Method] = []
    description: str = ""
    nested_classes: list[JavaClass] = []
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__,
            sort_keys=True, indent=4)

class JavaEnumConstant:
    name: str = ""
    description: str = ""
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__,
            sort_keys=True, indent=4)

class JavaEnum:
    java_import: str = ""
    name: str = ""
    methods: list[Method] = []
    constants: list[JavaEnumConstant] = []
    def toJSON(self):
        return json.dumps(self, default=lambda o: o.__dict__,
            sort_keys=True, indent=4)

def class_from_tag(tag: Tag, description: str) -> JavaClass:
    javaclass = JavaClass()
    javaclass.name = tag.find("a").text
    javaclass_parts: Match[str] = re.search(r"(.*?)(\(.*?\)|\s|\n)", tag.text)
    javaclass.name = javaclass_parts.group(0)
    args = javaclass_parts.group(1).split(",")
    for arg in args:
        print(arg)
        arg_parts = arg.split(" ")
        javaclass.args[arg_parts[1]] = arg_parts[0]

    javaclass.description = description
    return javaclass

def method_from_tag(tag: Tag, description: str, modifier: str, type: str) -> Method:
    method = Method()
    method.ty = type
    method.name = tag.find("a").text
    method.modifier = modifier
    method_parts: Match[str] = re.search(r"(.*?)(\(.*?\)|\s|\n)", tag.text)
    method.name = method_parts.group(0)
    args = tag.text.split(",")
    for arg in args:
        method.args[method_parts.group(1)] = method_parts.group(0)

    method.description = description
    return method

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
        item_html = requests.get(item_url)
        item_tree = BeautifulSoup(item_html.text, "html.parser")

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
                        nested_classes.append(class_from_tag(tag, description_tags[i]))
                    else:
                        methods.append(method_from_tag(tag, description_tags[i], modifier, type))

                    i += 1
                    print(methods)

                match title:
                    case "Class Hierarchy":
                        javaclass = JavaClass()
                        javaclass.java_import = parent
                        javaclass.name = tag.text
                        javaclass.methods = methods
                        javaclass.description = item_tree.find(id="class-description").find("div",{"class": "block"})
                        self.classes.append(javaclass)
                    case "Interface Hierarchy":
                        javainterface = JavaInterface()
                        javainterface.java_import = parent
                        javainterface.name = tag.text
                        javainterface.methods = methods
                        javainterface.nested_classes = nested_classes
                        javainterface.description = item_tree.find(id="class-description").find("div",{"class": "block"})
                        self.interfaces.append(javainterface)

            # we don't care about the annotation hierarchy

            case "Enum Hierarchy":
                enum_table: Tag = item_tree.find(id="enum-constant-summary")
                enum_tags: ResultSet = enum_table.find_all("div", {"class": "col-first"})
                description_tags: ResultSet = enum_table.find_all("div", {"class": "col-second"})

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

                methods = list[Method]

                i = 0
                for tag in method_tags:
                    if(tag.text == "Method" or tag.text == "Interface"):
                        continue

                    tag: Tag = tag.find("code")

                    # modifier
                    modifier_tag = modifier_tags[i]
                    modifier_parts = modifier_tag.split(" ")
                    modifier: str = ""
                    type: str = ""
                    if(len(modifier_parts) == 1):
                        modifier = "package-private"
                        type = modifier_tag.name
                    else:
                        modifier = modifier_parts[0]
                        type = modifier_parts[1]

                    method = method_from_tag(tag, description_tags[i], modifier, type)
                    methods.append(method)

                enums.methods = methods

                self.enumerators.append(enums)

    def toJSON(self):
        for c in self.classes:
            print(c.name)


def spigot_url_from_tag(parent: str, tag: Tag) -> str:
    if tag is None:
        raise Exception("tag provided is NoneType")
    parent = parent.replace("."+tag.text, "")
    parent_path = parent.replace(".","/")
    parent_path = re.sub(r"(\(.*?\)|<.*?>|\s|\n)", "", parent_path)

    return "https://hub.spigotmc.org/javadocs/spigot/org/bukkit/"+tag["href"]

def parse_javadoc(url: str) -> Javadoc:
    html = requests.get(url)
    tree = BeautifulSoup(html.text, "html.parser")
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
        n = 0
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
            if n == 5:
                break
            n += 1

    return doc

print("Parsing the javadoc into an object")
doc = parse_javadoc("https://hub.spigotmc.org/javadocs/spigot/org/bukkit/package-tree.html")
print(doc.toJSON())