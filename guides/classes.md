### Classes and instances   

Šimi is a fully object-oriented languages, meaning that classes are front and central. You'll spend a lot of time with them. All objects are instances of classes, including [raw objects and lists](objects_lists.md). [Numbers and Strings](basic_values.md) also get boxed into instances.

Define a class with the *class* keyword and a name:
```ruby
class MyClass

class ClassWithABody {
}
```

You can omit braces if the class doesn't have a body. Classes are constants, i.e you can't reassign the class' identifier to another value later on. By convention, class names are capitalized.

#### Methods
Methods are functions bound to an object. You declare them as regular methods within the class body:
```ruby
class MyClass {
    fn method1(a, b, c) {
        ...
    }

    fn method2() = self.method1(2, 3, 4)
}
```

Again, the main difference is that methods are bound to the object on which they're invoked - be it the class itself or its instance. This means that *self* won't point to the function, but to its caller.

Subclasses can override methods by re-declaring them with the same signature - same name, arity and argument composition.

If you want to have at least runtime assurance that a method will be overridden before it's used, have it return *AbstractMethodException*:
```ruby
class AbstractClass {
    fn pleaseOverrideMe(a, b, c) = AbstractMethodException()
    fn meToo(a, b) = AbstractMethodException()
}
```

#### Instantiating and initializers
To instantiate a class, just call it as you would a function:
```ruby
instance = MyClass()
```

Class instances are [immutable-to-the-outside](objects_lists.md) objects initialized according to the class spec ([open classes](#final-and-open-classes) create mutable instances, though). This means that instance fields can be modified from within, via *self*, but not elsewhere:
```ruby
class MyClass {
    fn setter {
        @field = 5
    }
}
instance = MyClass()
instance.setter() # Works!
print instance.field # 5
instance.field = 10 # Nope, runtime error.
```

Instantiating a class implicitly calls the **init** method. Even if you didn't define one, each class has an implicit, argument-less *init* with an empty body. Overriding *init* allows you to add additional initialization logic to your class, as well as to pass arguments to it:
```ruby
class MyDate {
    init(timestamp) {
        # other logic here
    }
}
date = MyDate(100505050)
```

Some notes on *init*:
* Since it's such an oft-used construct, you don't have to put *fn* before *init*, as that'd be tiresome. Of course, if that's your thing, you're more than welcome to write *fn init* every time.
* *init* is just a regular method, meaning it can have any number of arguments, including [default arguments](functions.md) and [type checks](safety_package.md). It also has access to *self* and *super*.
* *init* can't return early (that's a compile-time error) and always implicitly returns the instance at the end.

A common pattern is to pass values to the initializer in order to bind them to the instance's fields. Take, for example, the Range class:
```ruby
class Range {
    init(start, stop) {
        @start = start
        @stop = stop
    }
}
```

Šimi offers a simpler, more convenient syntax for this - if you wish to bind an *init* argument to a same-named field in the instance, just mark it with *@*:
```ruby
class Range {
    init(@start, @stop) # Same as the example above
}
```

*@* arguments don't differ from other arguments in any way, and can have default values and type checks. The code that binds these arguments to fields executes at *init* start.

If a class defines any initialized fields, they're assigned in the *init* body, after any bound arguments are set (with compiler synthesizing the code). This allows you to use *self* and *super* when assigning fields. Field assignments happen in the order in which they were typed.
```ruby
class MyClass is Supeclass {
    field1 = "some field"
    field2 = @field1.substring(2)
    field3 = super.method1()
}

# This if fully equivalent to:

class MyClass is Supeclass {
    init {
        @field1 = "some field"
        @field2 = @field1.substring(2)
        @field3 = super.method1()
    }
}
```

#### Native methods
Methods can be native, with their implementation left to the system Šimi's running on. Define native methods with *native* keyword instead of *fn*, and leave the body empty:
```ruby
native method(a, b, c = 3)
native another()
```

For more information on native modules and implementing native methods, see [native API guide](native_api.md).

You can also have a native *init*:
```ruby
native init(start, stop)
```

A native init has some caveats to it:
* Native *init* can't have bound arguments (with *@*).
* Just like other native methods, a native *init* receives the instance as its first argument (index 0).
* Native *init* must return the instance it was given, preferably after working on it a bit in the native code.

#### Inheritance
Šimi supports multiple inheritance. This is primarily because the language doesn't make a distinction between classes, abstract classes, interfaces, traits or other OOP constructs - everything is a class, regardless of it's empty or not, and if its methods are implemented.

List the superclasses after the *is* keyword, separated by commas:
```ruby
class Subclass is Superclass1, Superclass2, Superclass3
```

Superclasses are hierarchical, meaning the order in which you list them matters. The first one is the most important one, and the last one the least important. This hierarchy comes into play if two superclasses contain the same method/fiber/inner class - the more important superclass wins, and the subclass will inherit its method by default.

Each and every class you type will silently inherit the *Object* class, even if you don't specify it. This means that every instance has access to all the *Object* methods by default.

> *Design note:* I opted for *is* to indicate inheritance as I feel it's important to stress the *is a* relationship between superclasses and subclasses. Lot of inheritance overuse (== misuse) comes from the lack of understanding of this concept - only inherit if the type you're specifying *is a* type it inherits.

If you don't want your class to be inherited, [mark it final](#final-and-open-classes) with *class_*. For example, both *Num* and *String* are final classes.

You can access the superclass(es) and their fields with the *super* keyword. The super expression can have a specifier in parentheses to indicate which superclass should it refer to:
```ruby
class Subclass is Superclass1, Superclass2 {
    fn method {
        super # the Superclass1 class
        super.superClassMethod() # invokes the superclass method on Superclass1
        super(Superclass2).superClassMethod() # specifier specifies that we're looking for Superclass2 method
    }
}
```

Naturally, all *super* calls execute with *self* found to the subclass instance.

#### Mixins
Mixing-in works like inheritance does, but it doesn't create an *is a* relationship - the mixed-in class(es) just donate their methods to the mix, but aren't considered superclasses, and as such aren't accessible via *super* and fail the *is* test.

List the mixins after the superclasses list followed by *import*:
```ruby
class MyClass is Superclass import Mixin1, Mixin2
```

A mixin example is Core's *StreamMixin*:
```ruby
class_ StreamMixin {
    fn where(predicate) = Stream(self).where(predicate).toList()
    fn map(transformation) = Stream(self).map(transformation).toList()
}
```

Mixins are generally final since there's really no point in inheriting them, but that's a trivial matter.

The *Object* class contains this mixin, meaning these two methods are available to every object instance and to all of its subclasses. Also, an Object *is not* a StreamMixin.

Note that the *init* method is never mixed-in. Therefore, mixins can't donate fields since those are set in the initializer. Only methods, fibers, modules and inner classes can be added with mixing-in.

#### Final and open classes
For the sake of reference, there are three types of classes in Šimi:
1. Regular classes that can be inherited and whose instances are immutable.
2. Final classes that *can't be inherited* and whose instances are immutable:
```ruby
class_ FinalClassNoticeTheUnderscoreAfterClass
```
3. Open classes that can be inherited and whose instances are mutable:
```ruby
class$ OpenClassNoticeTheDollarSignAfterClass
```

All three of those have their time and place, but you'll spend most time with regular classes for the most part.

#### Implicit @ for getters
By now you know that setters always have @ before the field name - that's how the compiler knows that we're setting a field here instead of declaring/assigning to a local variable.

When it comes to getter, however, the compiler is a bit smarter and allows you to omit the @ when referring to instance fields. More specifically, when encountering an identifier that might either be a variable, or an instance field (a getter, just without @), it looks up its list of declared fields for this class. The list contains:
1. Any field, method, fiber or inner class declared *up to this point*.
2. All setters in the *init* method, including bound args.

Check out the Core class Stream and how it omits @ for getters, since the compiler knows what identifiers point to prior listed instance fields:
```ruby
class_ Stream {
    # ....

    init(@src) {
        @ops = List()
    }

    fn toMutableList {
        list = List()
        for item in src { # src is implicitly converted to @src
            dst = item
            for operation in ops { # ops is implicitly converted to @ops
       ### ... more code omitted
    }
}
```

#### Extensions
Extensions allow for a class to be extended with new methods after its declaration. Any class can be extended, even a final one. For example, see how the Core class *String* can be extended to replace newlines with HTML breaks:
```ruby
extend String {
    fn replaceNewlinesWithBreaks = replace("\n", "<br/>")
}

# Then, use the method!
replacedString = "a string\nwith\nnewlines\nyay!".replaceNewlinesWithBreaks()
```

To declare an extension, use the **extend** keyword followed by a class name, and then list the new methods you're adding to this class. You can add native methods as well. Also, methods that you add can be annotated. Naturally, implicit @ for getters works as well.

Since mixins just add methods to a class, you can add new mixins when extending:
```ruby
class MyMixin {
    ...
}

extend MyClass import MyMixin
```

Of course, you can add as many new mixins as you'd like while extending, and also mix-in some new methods as well:
```ruby
extend MyClass import MyMixin1, MyMixin2, MyMixin3 {
    fn newFn1 { ... }
    fn newFn2 { ... }

    !ImAnnotated(1, 2)
    native newNative { ... }
}
```

So, why extensions?
* They take care of the issue of the lack of forward declarations - in Šimi, you can't use something until you've declared it, and there are legitimate instances where two classes should make use of each other, but you can't because the other one is declared after the first one. Extensions take care of that.
* You can add convenience methods to existing classes and avoid the nasty syntax of standalone wrapper functions that take the target object as the first parameter. Furthermore, new methods allow you to change the internal state of an object via *internal mutability*.

A note about subclasses - since extensions are interpreted as they're encountered, *only subclasses declared after the extension will have the extension methods available*. In other words, if you declare an extension on the Object class, those methods won't magically appear in all other classes - just those that you declare after the extension took place. There's also a safety component to this - nobody can inject code into your class via extending its superclass.

#### Classes vs object literals
TODO revisit
As you learned by now, there is some overlap in the result of using classes vs object literals - classes are more powerful and allow your code to be nicely structured, but the end result in both cases is an object - the only difference is in its *class* field. So, when should you use which? Iterators often use object literals instead of dedicated classes.

Use classes when you need to superstructure. Use object literals for local objects with obvious functionality, such as class iterators, local return rsults, some annotations.