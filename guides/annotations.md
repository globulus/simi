### Annotations

Annotations allow you to associate *metadata* with classes and their members. You can access this data at runtime and use to in your code's business logic.

In simplest terms, an annotation is an object or a call proceeded by exclamation mark *!*:
```ruby
![type = "annotation1", value = 23]
class MyClass {
    ![data = [1, 2, 3]]
    someField = "default value"

    !AnnotationClass(1, "b", 3)
    fn method(a, b, c) {

    }
}
```

You can annotate classes and their members - fields, methods, fibers, etc. Each class or its member can have any number of annotations, listed below each other:
```ruby
![value = "this is 1st annotation"]
!ThisIsSecondAnnotation()
!THIRD_ONE_CONSTANTS_ARE_ALSO_ALLOWED
class MyClass
```

Annotations are evaluated at runtime, when the interpreter encounters them. There's no special pre-processing phase to take care of annotations, like there is in some other languages.

#### Accessing annotations at runtime
You can access annotations of a class and its members at runtime using the postfix operator *!*. It only works on classes - invoking it on anything else is a runtime error. If neither the class nor any of its members have any annotations, the operator returns an empty object (*[]*). Otherwise, it returns an object where keys are names and values are lists of annotations for that object (or objects themselves if it's an inner class).

Consider this example for more clarity:
```ruby
class AnnotationClass {
    init(@msg)

    fn toString = "Annotation \(@msg)"
}

![a = "class annot"]
!AnnotationClass("constructor annot")
class AnnotatedClass {
    !AnnotationClass("field annot")
    field = 5

    ![a = "fun annot"]
    fn a() {
        print "55"
    }

    ![a = "2nd fun annot"]
    fn b() {
        print "66"
    }
}
```

We define an annotation class (aptly named "AnnotationClass") to have a constructor call example. Then, we declare "AnnotatedClass", which, itself, has two annotations. Furthermore, its field and two methods are all annotated.

Then, if you were to get the annotations from the class like this:
```ruby
print AnnotatedClass! # ! is a postfix operator that gets the annotations
```

you'd get back an object like this:
```ruby
[AnnotatedClass = [[a = class annot], Annotation constructor annot], field = [Annotation field annot], a = [[a = fun annot]], b = [[a = 2nd fun annot]]]
```

It tells you that:
1. AnnotatedClass has a list of two annotations - the first one is an object, the other one the AnnotationClass instant. Notice how AnnotationClass's toString() is invoked.
2. "field" has a single AnnotationClass instance as its annotation.
3. Methods "a" and "b" have objects as their annotations as well.

Again, to recap, you get an object back where keys are class/field/method names, and values are lists of annotations for that particular key.

Note that you also get all the annotations for all the superclasses and their fields as well.

Consider the following example that generates SQL based on annotated model classes.

First, define two classes that we'll use for annotations: DbTable and DbField:
```ruby
class DbTable {
    init(@name = nil) # A table is just defined by a name
}
class DbField {
    init(@type, @name = nil, @primaryKey = false) # A column has a type, inferrable name and can be a primary key
}
```

Next, add a "Model" superclass (because all the models will have an "id" primary key), and two table classes:
```ruby
class Model {
    !DbField(Num, nil, true)
    id = 0
}

!DbTable("Users")
class User is Model {
    !DbField(String, "first_name")
    firstName = ""

    !DbField(String, "last_name")
    lastName = ""
}

!DbTable("Appointments")
class Appointment is Model {
    !DbField(String)
    userId = ""

    !DbField(Date, "timeslot")
    time = Date()
}
```

Here's the code for the class that actually generates the SQL. Read through the comments to see how annotations are extracted and parsed:
```ruby
class DbLib {
    init(tables) {
        sqlBuilder = String.builder() # Use a String.builder() when concatenating a lot of strings together, for performance reasons
        for table in tables {
            @sqlForTable(table, sqlBuilder)
        }
        @sql = sqlBuilder.build() # Extract the string from the builder with build()
    }

    fn sqlForTable(table, sqlBuilder) {
        ans = table! # Get the annotations from the provided class
        tableName = "" + table # Convert the class name to string
        for tableAn in ans.(tableName).where(=$0 is DbTable) { # Find the DbTable annotation in the class annotations
            name = tableAn.name ?? tableName # Infer the table if not provided
            sqlBuilder.add("CREATE TABLE ").add(name).add(" (\n")
            @sqlForFields(ans.where(=$0 != tableName), sqlBuilder) # Process all the annotations that don't refer to the table class, but its fields instead
            sqlBuilder.add(");\n")
        }
    }

    fn sqlForFields(ans, sqlBuilder) {
        for [field, fieldAns] in ans.zip() {
            dbFieldAn = fieldAns.where(=$0 is DbField).0 ?! {
                continue
            }
            name = dbFieldAn.name ?? field
            type = when dbFieldAn.type {
                Num = "int"
                String = "varchar(255)"
                Date = "date"
                else = nil
            }
            sqlBuilder.add(name).add(" ").add(type)
            if dbFieldAn.primaryKey {
                if type == "int" {
                    sqlBuilder.add(" NOT NULL AUTO_INCREMENT,")
                }
                sqlBuilder.add("\nPRIMARY KEY (").add(name).add("),")
            } else {
                sqlBuilder.add(",")
            }
            sqlBuilder.add("\n")
        }
    }
}
```

Let's test the DB lib class on the table classes:
```ruby
print DbLib([User, Appointment]).sql

# The output is
CREATE TABLE Users (
id int NOT NULL AUTO_INCREMENT,
PRIMARY KEY (id),
first_name varchar(255),
last_name varchar(255),
);
CREATE TABLE Appointments (
id int NOT NULL AUTO_INCREMENT,
PRIMARY KEY (id),
userId varchar(255),
timeslot date,
);
```