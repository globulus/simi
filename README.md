# Šimi - an awesome programming language

Šimi (*she-me*) is small, object-oriented programming language that aims to combine the best features of Python, Ruby, JavaScript and Swift into a concise, expressive and highly regular syntax. Šimi's interpreted nature and built-in metaprogramming operators allow the code to be updated at runtime and features to be added to the language by anyone!

You can run Šimi on any machine that has JVM by invoking the Simi JAR, which involves virtually all servers and desktop computers. There's also native support for devices running [Android](https://github.com/globulus/simi-android) or [iOS](https://github.com/globulus/simi-ios). You can also [write a server in Šimi!](https://github.com/globulus/simi-sync/tree/master/web).

What Šimi offers:
* Modern, powerful and expressive syntax.
* Highly regular grammar - reduce cognitive load.
* Full OOP support.
* [Metaprogramming operators](#metaprogramming-and-deserialization---gu-and-ivic) allows for language extensions and syntax sugar to be added by anyone.
* Rich [Java API](#java-api) - if it exist in Java, you can easily bridge it into Šimi!
* Run Šimi on any device that supports JRE, with special native support in Android.
* Run natively on any Cocoa machine ([iOS or Mac OS X](https://github.com/globulus/simi-ios)).
* [Debugger included.](#debugger)
* Free to use and modify!

### Quickstart

1. At the very least, download [Šimi JAR](simi.jar). Downloading [the shell script](simi.sh) makes running even easier.
2. Download the [Stdlib folder](Stdlib).
3. Invoke the shell script with the source Šimi file:
```bash
./simi.sh input.simi
```
4. Profit!

- Table of contents
    + [Basic syntax](#basic-syntax)
      - [Keywords](#keywords)
      - [Comments](#comments)
      - [Identifiers](#identifiers)
      - [Newlines](#newlines)
      - [pass](#pass)
      - [Code blocks](#code-blocks)
      - [Variables and constants](#variables-and-constants)
    + [Values](#values)
      - [Numbers](#numbers)
      - [Strings](#strings)
        * [Boxed numbers and strings](#boxed-numbers-and-strings)
      - [Functions](#functions)
        * [Implicit return](#implicit-return)
        * [Implicit parameters](#implicit-parameters)
        * [Closure vs. environment invocation](#closure-vs-environment-invocation)
      - [Objects](#objects)
        * [Dictionaries and Arrays - the Siamese Twins](#dictionaries-and-arrays---the-siamese-twins)
      - [Value conversions](#value-conversions)
    + [Classes](#classes)
      - [Using classes as modules](#using-classes-as-modules)
    + [Operators](#operators)
      - [Arithmetic](#arithmetic)
      - [Assignment](#assignment)
      - [Logical](#logical)
      - [Comparison](#comparison)
      - [is and is not](#is-and-is-not)
      - [in and not in](#in-and-not-in)
      - [?? - nil coalescence](#---nil-coalescence)
      - [? - nil check](#---nil-check)
      - [@ - self referencing](#---self-referencing)
      - [Bitwise operations](#bitwise-operations)
    + [Control flow](#control-flow)
      - [Truth](#truth)
      - [if-elsif-else](#if-elsif-else)
      - [when](#when)
      - [*if* and *when* expressions](#if-and-when-expressions)
        * [*ife* and lazy loading](#ife-and-lazy-loading)
      - [while loop](#while-loop)
      - [for-in loop](#for-in-loop)
      - [Iterators and iterables](#iterators-and-iterables)
      - [break and continue](#break-and-continue)
      - [return and yield](#return-and-yield)
      - [Async programming with yield expressions](#async-programming-with-yield-expressions)
    + [Exception handling](#exception-handling)
      - [Exceptions](#exceptions)
      - [The *rescue* block](#the-rescue-block)
    + [Enums](#enums)
    + [Importing code](#importing-code)
    + [Java API](#java-api)
    + [Annotations](#annotations)
    + [Metaprogramming and (de)serialization - *gu* and *ivic*](#metaprogramming-and-deserialization---gu-and-ivic)
    + [Debugger](#debugger)
        - [Breakpoints](#breakpoints)
        - [Visual debugging](#visual-debugging)
    + [Basic modules](#basic-modules)
        - [Stdlib](#stdlib)
        - [File](#file)
        - [Net](#net)
        - [Test](#test)
        - [SMT](#smt)
        - [SQL and ORM](#sql-and-orm)
        - [CodeBlocks](#codeblocks)
    + [Android integration](#android-integration)
    + [iOS integration](#ios-integration)
    + [To-Dos](#to-dos)
    + [Keyword glossary](#keyword-glossary)

### Basic syntax

#### Keywords
Šimi has 26 reserved keywords:
```ruby
and break class continue def do else false fib for gu if import
in is ivic native nil not or print return self super true when while yield
```

Check out the [Keyword glossary](#keyword-glossary) for a quick rundown of their use cases.

#### Comments
```ruby
# This is a single line comment
print b # They span until the end of the line

#* And here is
a multiline comment
spanning multple lines *#
```

#### Identifiers
Identifiers start with a letter, or _, and then may contain any combination of letters, numbers, or underscores. Identifiers are case-sensitive. Note that identifiers that start with _ have special meanings in some instances.

#### Newlines
Line breaks separate Šimi statements and as such are meaningful. E.g, an assignment statement must be terminated with a newline.

If your line is very long, you can break it into multiple lines by ending each line with a backslash (\\) for readability purposes.

On the other hand, if you wish to pack multiple short statements onto a single line, you can separate them with a semicolon (;). To the lexer, a newline and a semicolon are the same token.
```ruby
a = 5 # This is a line
# Here are two lines, the first one nicely formatted by using \
arr = [1, 2, 3]\
    .reversed()\
    .joined(with = [5, 6, 7])\
    .where(def i = i <= 5)\
    .map(def i = i * 2)\
    .sorted(def (l, r) = r.compareTo(l))
print arr; print a # And here are two print statements separated by ;
```

#### Code blocks
A block of code starts with a left brace *{*, followed by one or multiple statements, terminated with a right brace *}*.
```ruby
if a < 5 { print a } # Single line block
else { # Multiline block
    a += 3
    print b
}
```

#### Scoping
Šimi uses lexical scoping, i.e a variable is valid in the block it was declared in and its parent blocks:
```ruby
a = 5
{
    print a # Works, a is visible in a nested block
    b = 6
    print b # Yup, b is visible here as well
}
print a # Works
print b # Compile error, b is not declared in this scope

```

**Name shadowing is prohibited and results in a compile error.** This is to prevent errors...
```ruby
a = 5
{
    a = 6 # Compile error, a was already declared in an outer block
}
```

#### Variables
In Šimi, variables are declared with the *=* operator:
```ruby
a = 5
b = "string"
c = true
```

To assign a new value to a variable, you need to use *$=* operator instead of *=*:
```ruby
a = 5
a $= 6 # The value of a was changed
a = 5 # Compile error, variable redeclared
```

> The reasoning behind is that most variables you declare are effectively constants, i.e they're just temporary placeholders for a value. This premise is present in languages such as Swift and Kotlin, where "let" and "val" are preferred over "var", until you find out that a variable's value indeed needs to change. In Šimi, assignment is even more explicit with $=.

Compound operators (+=, -=, etc.) are invoked with $= by default. Also, $= cannot be used with set expressions as they're mutating by default.

##### Constants

Some variables are real constants and using $= with them will result in a compiler error. These are:
1. Variables whose name is in CAPS_SNAKE_CASE.
2. Declared classes, functions and fibers, regardless of their case.
3. Variables declared with *_=* instead of *=*. The *_=* operator was primarily introduced to allow the SINGLETON OBJECTS LINK to be declared as constants.

```ruby
THIS_IS_A_CONST = 5
THIS_IS_A_CONST $= "error"

def func() {
    return 2
}
func $= "this is also an error"

realConst _= 10
realConst $= "again, a compile time error"
```

### Values

Šimi supports exactly 5 values, making it extremely easy to infer types and know how does a certain part of code works:
1. Number - represents 64-bit number, either integer or floating-point. Also serves as the boolean type, with *true* mapping to 1, and *false* mapping to 0.
2. String - represents a (multiline) piece of text, enclosed in either single or double quotes.
3. Function - a parametrized block of code that can be called. Functions are first-class citizens in Šimi and can be passed around as normal values.
4. Object - the most powerful Šimi construct, represents a mutable or immutable array/list, set, dictionary/map/hash, or a class instance.
5. Nil - represents an absence of value. Invoking operations on nils generally results in a nil, unless the [nil check operator is used](#---nil-check).

#### Numbers
At surface, Šimi doesn't make a distinction between integers and floating-point numbers, but instead has one number type, 8 bytes long, which also doubles as a boolean type where 0 represents a false value.

Internally, Šimi uses *either* a 64-bit integer (long), or a 64-bit floating-point (double) to store the numerical value passed to it, depending on if the supplied value is an integer or not. This allows for precision when dealing with both integer and decimal values, and requires attentiveness when creating Number instances via native calls. When an operation involves two numbers, the result will be integer of both values are internally integers, otherwise it'll be a decimal number.

When using numbers as objects, they're boxed into an instance of open Core class *Num*, which contains useful methods for converting numbers to strings, rounding, etc.

Numbers are pass-by-value, and boxed numbers are pass-by-reference.

#### Strings
Strings are arrays of characters. They can be enclosed either in single or double quotes. You can freely put any characters inside a string, including newlines. Regular escape characters can be included.
```ruby
string = "this is a
    multiline
 string with tabs"
anotherString = 'this is another "string"'
```

String interpolation is supported by enclosing the nested expressions into *\\(SOMETHING)*:

```ruby
a = 2
b = 3
print "a doubled is \(a * 2) and b minus 1 halved is \((b - 1) / 2)"
# Prints: a doubled is 4 and b minus 1 halved is 1
```

When used as objects, strings are boxed into an instance of open Core class *String*, which contains useful methods for string manipulation. Since strings are immutable, all of these methods return a new string.

Strings are pass-by-value, and boxed strings are pass-by-reference.

The String class contains a native *builder()* method that allows you to concatenate a large number of string components without sacrificing the performance that results from copying a lot of strings:
```ruby
class Range {

    ... rest of implementation omitted ...

    def toString = String.builder()\
            .add("Range from ").add(@start)\
            .add(" to ").add(@stop)\
            .add(" by ").add(@step)\
            .build()
}
```

##### Boxed numbers and strings
Boxed numbers and strings are objects with two fields, a class being Num or String, respectively, and a private field "_" that represents the raw value. The raw value can only be accessed by methods and functions that extend the Stdlib classes, and is read-only. Using the raw value alongside the @ operator results in the so-called *snail* lexeme:
```ruby
# Implementation of times() method from Number class
def times = ife(@_ < 0, =Range(@_, 0), =Range(0, @_)).iterate()
```

#### Functions
A function is a named or unnamed parametrized block of code. Here are valid examples of functions:
```ruby
# A free-standing function with two params
def add(a, b) {
    c = a + b
    return c
}

# A function stored in a variable, equivalent to the example above
subtract = def (a, b) {
    c = a - b
    return c
}

# Functions that map to native code have a single native statement
# in their bodies
def pow(a, b) = native

# Functions without parameters aren't required to have parentheses
def printFunction {
    print "printing"
 }

# A lambda function passed to filter an array, has implicit return if it's single line
# Lambdas with a single parameter needn't put the parameter in parentheses
filteredArray = [1, 2, 3, 4, 5].where(def i = i > 2)

# There is a shorthand syntax for parameterless lambdas.
# You can think of this syntax as storing uninterpreter Šimi code that
# can be invoked later on. This pattern is heavily used with lazy loading
# and the ife function.
storedExpression = =2+2 # Parses into: def = 2 + 2

# You can use the shorthand syntax with implicit params as well
filteredArray = [1, 2, 3, 4, 5].where(=_0 > 2)
```
Functions start with the *def* keyword. Native functions are required to have a single *native* statement in their bodies.

Functions in class bodies are called *methods* and have a few caveats to them, which we'll cover in the Classes section.

Functions can be invoked by using (), and passing arguments in them. You can *name* any parameter in the function by putting a name and an equals sign before the value. This helps readability, and the Šimi interpreter will disregard anything up to and including the equals sign for a given parameter.
```ruby
sum = add(3, 4)
difference = subtract(a = 5, b = pow(2, 3))
printFunction()
```

For functions that take 0 or 2 or more arguments, you can invoke them by passing an object whose size is the same as the number of expected arguments. The object will be decomposed by the interpreter, and its values passed as function parameters. This allows both functions and their invocations to be fully dynamic:
```ruby
def f(a, b, c): pass
f(1, 2, 3) # The conventional way of doing it
f([4, 5, 6]) # Invoking with an array
f([a = 7, b = 8, c = 9]) # Invoking with dictionary
# All these invocations do the same thing!
```
Obviously, argument decomposition can't be applied to functions that take one argument, as the interpreter can't know whether it should unpack the argument or not.

You can check if a value is a function by testing agains the *Function* class:
```ruby
fun is Function
```

If you need to access a function from within itself, you can use the special *self(def)* construct:
```ruby
def f(a) {
    objectSelf = self # Resolves to invoked object (or nil)
    functionSelf = self(def) # Returns function f
}
```

There's a syntax sugar that allows for static type checking of selected parameters:
```ruby
# Note the x is Type syntax to enforce static type checking
# Checked and unchecked params can be freely mixed
def f(a is Number, b is Range, c) {
    print "something"
}
```
The above function is upon parsing internally stored with prepended type checks that raise *TypeMismatchException*:
```ruby
def f(a, b, c) {
    if a is not Number {
     TypeMismatchException(a, Number).raise()
    }
    if b is not Range{
     TypeMismatchException(b, Range).raise()
    }
    print "something"
}
```

##### Implicit return

If a function body is declared with equals sign *=*, it is allowed to hold a **single expression** and its result is implicitly returned, i.e:
```ruby
    def fun(a, b) = a + b
    # equals
    def fun(a, b) {
        return a + b
    }
```

An exception to this rule are **setter functions**, i.e those functions that being with *set* and have a *single parameter*.

```ruby
 def setValue(value) = pass # Setters don't fall in this category
 def setter(value) = @val = value # Setters don't fall in this category
 ```

Functions with proper blocks implicitly **return self**. This allows for chaining calls to methods that don't return a value and/or perform an object setup.

```ruby
class Button {
    def setTitle(title) = pass # Setters have implicit return self
    def setBackgroundColor(color) = pass
    def setOnClickListener(listener) = pass
    def performClick() {
        @onClickListener(self) # Implicit return of function call
    }
}

button = Button()\ # Implicit return self from init
    .setTitle("Click me")\
    .setBackgroundColor(Color.WHITE)\
    .setOnClickListener(def sender: print sender.title)
# More code...
button.performClick()
```
It also forces the programmer to explicitly use *return nil* if they want for nil to be returned, therefore listing out all the possible values a function can return.

##### Implicit arguments

While using the shorthand function definition syntax, you can use **implicit aruments**. Implicit params' names start with *_* and are followed by digits: *_0, _1, _2, ...*, with _0 being the first parameter, _1 the second, etc:

```ruby
sumThree = =_1 + _0 + _2
```

internally translates to:

```ruby
sumThree = def (_0, _1, _2) { return _1 + _0 + _2 }
```

This syntax nicely abbreviates often-used lambdas that programmers are familiar with, such as those passed to 2nd-order functions of the *Object* class. Consider the array transformation example from earlier on:

```ruby
arr = [1, 2, 3]\
    .where(def i = i <= 5)\
    .map(def i = i * 2)\
    .sorted(def (l, r) = r.compareTo(l))
```

abbreviated by using implicit args:

```ruby
arr = [1, 2, 3]\
    .where(=_0 <= 5)\
    .map(=_0 * 2)\
    .sorted(=_1.compareTo(_0))
```

Of course, the implicit args syntax can be used with any shorthand lambda, but improvements to conciseness shouldn't be made at the expense of readability.

#### Objects
Objects are an extremely powerful construct that combine expressiveness and flexibility. At the most basic level, every object is pair of a collection of key-value pairs, where key is a string, and value can be any of 4 Šimi values outlined above, and an indexed array, which also can contain any Šimi value. **Nils are not permitted as object values, and associating a nil with a key or index will delete that key from the object.** This allows the Objects to serve any of the purposes that are, in other languages, split between:
1. Class instances
2. Structs
3. Arrays/Lists
4. Sets
5. Tuples
6. Dictionaries/Maps/Hashes
7. Static classes

The underlying class for objects is the open Stdlib class Object. It contains a large number of (mostly native) methods that allow the objects that facilitate their use in any of the purposes in the list above.

Objects can be *mutable* or *immutable*:
* You can change the values of fields of mutable objects, including deleting them (by setting to nil). You may introduce new fields to mutable objects.
* Immutable objects are fixed, and the values of their fields can only be manipulated by methods of their classes.

Objects have a rich literal syntax:
```ruby
# A class instance, immutable by default (See Classes section)
pen = Pen("blue")

# A value class/struct (which can also be done via Class instancing)
point = [x = 10, y = 20]

# An array/list is created as a regular object, but without any keys
mutableList = $[1, 2, 3, 4, 5]
mutableList.push(6)
immutableList = ["str", "str2", "str4"]

# Objects have unique keyes by default, and you can remove duplicates
# from an array via the native unique() method
set = [2, 2, 1, 5, 5, 3].uniques().sorted()

# Tuples are basically immutable lists/arrays
tuple = [10, 20, "str"]

# Dictionary/Map/Hash
mutableObject = $[key1 = "value1", key2 = 12, key3 = [1, 2, 3, 4]]
immutableObject = [key4 = mutableObject.key3.len(), key5 = 12.345]

# Immutable objects with constants and functions serve the role of
# static classes in other languages.
basicArithmetic = [
    add = def (a, b) = a + b
    subtract = def (a, b) = a - b
]

# You can also have composite objects, that contain both associated
# and indexed values
compositeObject = [a = 1, 2, b = 3, 4, c = 5, 6]
```
Object literals are enclosed in brackets (\[ ]), with mutable object having a $ before the brackets ($\[ ]).

Getting and setting values is done via the dot operator (.). For editing the array part, the supplied key must be a number, whereas for the dictionary part it can either be an identifier, a string or a number.
```ruby
object = $[a = 2, b = 3]
a = object.a # Gets value for key a
b = object.0 # Gets the first value
c = object.("" + "a") # You can evaluate keys by encolsing them in ()

object.c = "str" # Added a new key-value pair to the object
object.b = nil # Removed a property from the object

array = $[1, 2, 3, 4, 5]
d = array.1 # Gets the second element in the array
array.push(6)
array.4 = nil # Removed the fifth element from the array
```

Objects are pass-by-reference.

There is an object decomposition syntax sugar for extracting multiple values out of an object at once:
```ruby
obj = [a = 3, b = 4, c = 5]
[a, b, d] = obj
# Above compiles down to
# a = obj.a ?? obj.0
# b = obj.b ?? obj.1
# d = obj.d ?? obj.2
print a # 3
print b # 4
print d # 5
```

##### Dictionaries and Arrays - the Siamese Twins
Šimi's composite objects feature may confuse people coming from other languages, where dictionaries/maps/hashes and arrays/lists are separate entities. To reiterate, every Šimi object always has **both a dictionary and an array**. Admittedly, the instances where you need to use both part simultaneously are rare, so it's not necessary to deeply understand its inner workings and edge cases that arise because of this approach.

Here's a quick example that showcases a composite object in action:
```ruby
# You can freely mix associated and unassociated values
compositeObj = [a = 3, 2, b = 4, 3, c = 5, 4, 6]
print compositeObj
# Prints
#[
#	immutable: true
#	class: Object;
#	a = 3
#	b = 4
#	c = 5
#	0 = 2
#	1 = 3
#	2 = 4
#	3 = 6
#]

# Composite object literals are (de)serializable
print (gu ivic compositeObj) <> compositeObj # true

print compositeObj.len() # Prints total number of elements = 7
print compositeObj.keys() # Keys are [a, b, c]
print compositeObj.values() # Values are [3, 4, 5, 2, 3, 4, 6]
print compositeObj.reversed() # Reverses both the array and dictionary separately
print compositeObj.sorted() # Sorts both parts separately

# forEach uses an iterator and as such works either with dictionary
# or array part, depending on which one is present (dictionary has
# higher precedence). The same is true for other higher-order functions.
compositeObj.forEach(def v { print v })

# Enumerates ALL the values - dictionary first, and then array, and as
# such can be used to iterate through entire composite object.
print compositeObj.zip()

# Ruler is a shallow copy of the object's array part. Modifying the
# rules modifies the object as well.
ruler = compositeObj.ruler()
print ruler
ruler.append(100)
print compositeObj
```

#### Value conversions

Despite being a dynamic language, Šimi doesn't do implicit type conversions. The only thing similar to an implicit conversion is concatenating a value to a string, in case of which this value will be *stringified*. The same is true when a value is printed using the *print* statement.

Stringification works in the following way:
* For numbers and *nil*, you get what you expect.
* All other objects have a default, natively implemented toString() method, that can be overridden.

* Strings can be converted to Numbers by using the toNumber() method, but be aware that invoking this method may produce NumberFormatException that needs to be handled via the *rescue* block.

### Classes
Šimi is a fully object-oriented languages, with classes being used to define reusable object templates. A class consists of a name, list of superclasses, and a body that contains constants and methods:
```ruby
class Car(Vehicle, in ClassToMixin) {
    wheels = 4

    def init(brand, model, year) = pass

    def refuel(amount) {
        @fuel = Math.min(tank, fuel + amount)
    }

    def drive(distance) {
        fuelSpent = expenditure * distance
        @fuel = Math.max(0, fuel - fuelSpent)
    }
}

car = Car("Peugeot", "2008", 2014) # Creating a new class instance
print car.brand # Peugeot
```
Here's a quick rundown of classes:
* By convention, class names are Capitalized.
* Šimi supports multiple inheritance, i.e a class can have any number of Superclasses. This is partly because Šimi doesn't make a distinction between regular classes, abstract classes and interfaces. When a method is requested via an object getter, the resolution is as following:

    1. Check the current class for that method name.
    2. Check the leftmost superclass for that method name.
    3. Recursively check that superclass' lefmost superclass, all the way up.

* Using the *in* keyword before a class name in superclasses list will include the provided class instead of inheriting it, thus creating a mixin by copying all the public, non-init fields and methods from the supplied class into the target class.
* All classes except base classes (Object, String, Number, Function, and Exception) silently inherit the Object class unless another superclass is specified. This means that every object, no matter what class it comes from, has access to Object methods.
* You can access the superclass methods directly via the *super* keyword. The name resolution path is the same as described in multiple inheritance section. If multiple superclasses (Object included) override a certain method, and you want to use a method from a specific superclass, you may specify that superclass's name in parentheses:
```ruby
class OtherCar(Car, Range) { # This combination makes no sense :D
    def init() {
        super.init(10, 20) # Uses the init from Car class
        @start = 5
        @stop = 10
        @step = 2
        super.refill(2) # Refill from superclass
        @refill(3) # OtherCar implementation of refill
   }

   def refill(amount) {
        print "Doing nothing"
   }

   def has(value) {
    # Here we specify that we wish to use the has() method from the Range class
    return super(Range).has(value)
   }
}
```
* Classes themselves are objects, with "class" set to a void object named "Class". You can check if an Object is a class by using **VAR is Class**, even though Class itself normally evaluates to *nil*.
* From within the class, all setters must use self or @ (i.e, self.fuel = 3, or @fuel = 3). Instance fields outside of setter may or may not use self-referencing (although it's recommended for legibility).
* Instance vars and methods are mutable by default from within the class, and don't require usage of the $= operator.
* Class instances are immutable - you cannot add, remove or change their properties, except from within the class methods.
* Class methods are different than normal functions because they can be overloaded, i.e you can have two functions with the same name, but different number of parameters. This cannot be done in regular key-value object literals:
```ruby
class Writer { # Implicitly inherits Object
    def write(words) {
     print words
    }
    def write(words, times) {
        for i in times.times() {
         @write(words) # method chaining
        }
    }
}
```
* Constructor methods are named *init*. When an object is constructed via class instantation, the interpeter will look up for an *init* method with the appropriate number of parameters. All objects have a default empty constructor that takes no parameters.
* An empty init with parameters is a special construct that creates instance variables for all the parameters. This makes it very easy to construct value object classes without having to write boilerplate code.
```ruby
class Point {
    def init(x, y): pass

    # This is fully equivalent to:
    # def init(x, y) {
    #   @x = x
    #   @y = y
    # }

    # In value classes, you may want to override the equals method
    # to check against fields, i.e to use matches() with == operator:
    def equals(other) = self <> other
}
```
* Similarly, an empty method that starts with *set* and takes a single parameter will be autofilled with a setter for the given value.
```ruby
class Person {
    def setId(id) = pass
    def setName(n is String) = pass

    # This is fully equivalent to:
    # def setId(id) {
    #    @id = id
    # }
    # def setName(n is String) {
    #   if n is not String {
    #    TypeMismatchException(n, String).raise()
    #   }
    #   @name = n
    # }
}
```
* All methods in Šimi classes are at the same time static and non-static (class and instance), it's their content that defines if they can indeed by used as both - when referencing a method on an instance, *self* will point to that instance; conversely, it will point to the Class object when the method is invoked statically.
    + Do note that instance objects **are mutable** within static methods of their class or its subclasses. Consider the following example:
   ```ruby
   class Person
   class Car

   class Audi(Car) {
      def staticMethod {
         audiInstance = Audi()
         car.weight = 1800 # Works, self is Audi class
         carInstance = Car()
         car.weight = 1500 # Works, self is Audi class which is subclass of Car
         personInstance = Person()
         person.weight = 80 # Error, self is Audi class which isn't subclass of Person
      }
   }
   ```
* Methods and instance variables that start with an *underscore* (_) are considered protected, i.e they can only be accessed from the current class and its subclasses. Trying to access such a field raises an error:

```ruby
class Private {
    def init() {
        self._privateField = 3
    }

    def _method() {
        self._privateField = 2 # Works
    }
}

private = Private()
print private._privateField # Error
print private._method() # Error
```
* Classes that are defined as **class$ Name** are *open classes*, which means that you can add properties to them. Most base classes are open, allowing you to add methods to all Objects, Strings and Numbers:
```ruby
# Adding a method that doubles a number
Number.double = =@_ * 2
a = 3
b = a.double() # b == 6
```
* Classes that are defined as **class_ Name** are *final classes*, which can't be subclassed. This feature is important as it normally all subclasses can alter instance fields from their superclasses, so being able to make classes non-subclassable adds to code safety and stability.
* The Object class has a native *builder()* method, meaning that any class in Šimi automatically implements the builder pattern:
```ruby
Car = Car.builder()\
    .brand("Mazda")\
    .model("CX-7")\
    .year(2009)\
    .build()
```

#### List and object comprehensions

Comprehensions make generating lists and object based on other data quicker to write, read and execute. Imagine you're tasked with generating a list of squares of even numbers up to 10. One route you might take is to use a for loop:
```ruby
list = List()
for i in 1..10 {
    if i % 2 == 0 {
        list.add(i * i)
    }
}
```

A list comprehension makes this much more concise:
```ruby
list = [for i in 1..10 if i % 2 == 0 do i * i]
```

The *if* part is optional:
```ruby
list = [for item in otherList do item + " something"]

# You can create both mutable and immutable lists with comprehensions,
# just put $ in front:
mutableList = $[for item in otherList if item > 10 do item] 
```

An object comprehension has to provide a key-value pair, separated by *=*:
```ruby
object = [for i in 1..5 do "key\(i)" = i]
# Just like in regular for-loops, object decomposition can be used here as well
object2 = $[for [k, v] in otherObject if k not in ForbiddenKeys and v < 10 do k = v * 10]
```

In most cases when mapping a list or an object from another list or object, the list comprehension is semantically equal to a filtering followed by mapping:
```ruby
list = other.where(=_0 < 10).map(=_0 * 2)
# this is exactly the same as
list = [for i in other if i < 10 do i * 2]
```

The comprehensions have a few advatanges, though:
1. You can specify if the created list/object is mutable or not.
2. Objects can filter and map keys and values at the same time.
3. They're easier to read and type.
4. They're much faster to execute due to the way they're compiled.

### Operators

#### Arithmetic

+, -, *, /, //, %

* \+ can be used to add values and concatenate strings.
* Other operators work on Numbers only.
* \- Can be used as an unary operator.
* // is the integer division operator, 3 // 2 == 1, while 3 / 2 == 1.5.

#### Assignment

=, $=, +=, -=, *=, /=, //=, %=, ??=

#### Logical

not, and, or

* *not* is unary, *and* and *or* are binary.
* *and* and *or* are short-circuit operators (and-then and or-else).

#### Comparison

==, !=, <, <=, >, >=, <>

* On objects, == implicitly calls the *equals()* method. By default, it checks if two object *references* are the same. If you wish to compare you class instances based on values, override this method in your class. If you need to check equivalence based on values, check out the *matches()* method.
* The matching operator <> implicitly invokes *matches()* method. The default implementation of this method checks objects by their keys and values, basically checking their equivalence. You can, of course, override this method to accept any combination of arguments and perform different matching, e.g matching strings based on regular expressions.
* <, <=, > and >= can only be used with Numbers.

> I'm aware that using "!=" for "not equal" seems out of place, especially since "not" is a separate keyword and "!" is used in other capacities elsewhere in the language, but it just seems natural coming from C.

#### Range

*Range* is a Core class that is used quite often, so it has its own dedicated set of operators that operate on numbers.

* *..* (two dots) creates a range from left hand side *until* right hand side - up to, but not including:
```ruby
1..10 # 1, 2, 3, 4, 5, 6, 7, 8, 9
```
* *...* (three dots) creates a range from left hand side *to* right hand side - up to, and including:
```ruby
1...10 # 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
```

Internally, these operators are mere syntax sugar that compiles as invocations of Num.rangeTo and Num.rangeUntil methods, respectively.

#### is and is not
You can check if a value is of a certain type with the *is* operator. Its inverted version is *is not*. The right hand side must be a class. Since everything in Šimi is an object (or can be boxed into one), you can dynamically check the type of anything on the left hand side:
* *nil is Anything* always returns false.
* For numbers: var is Num
* For Strings: var is String
* For functions: var is Function
* For fibers: var is Fiber
* For classes: var is Class
* For objects: var is Object
* For lists: var is List
* To check is an object is an instance of a class or any of its subclasses: var is SomeClass.
* You can also use *is* to see if a class is a subclass of another class: SomeClass is PotentialSuperClass.
    * > This part is debatable, as *is* is supposed to check *what type a value is*, but then again, it flows well with the *is* keyword for subclassing. It might be removed in the future, or replaced with *>* (although that one isn't without issues either).

```ruby
a = [1, 2, 3, 4]
a is Object # true
a is not Number # true
b = 5
b is Number # true
b is String # false
car = Car("Audi", "A6", 2016)
car is Car # true
car is not Object # false

TODO expand
```

#### in and not in
The *in* operator implicitly calls the *has()* method. that's defined for Objects and Strings, but not for numbers. For strings, it checks presence of a substring. For Objects, it checks presence of a value for arrays, and key for keyed objects. It can be overriden in subclasses, for example in the Range class:
```ruby
class Range {

    # ... rest of implementation omitted ...

    def has(val) = if @start < @stop {
            val >= @start and val < @stop
        } else {
            val <= @start and val > @stop
        }
    }
}
"str" in "substring" # true
"a" not in "bcdf" # true
2 in [1, 2, 3] # true
"a" in [b = 2, c = 3] # false
range = Range(1, 10)
2 in range # true
10 not in range # true
```

#### ?? - nil coalescence
The ?? operator checks if the value for left is nil. If it is, it returns the right value, otherwise it returns the left value. This operator is short-circuit (right won't be evaluated if left is not nil).
```ruby
a = b ?? c # is equivalent to a = ife(b != nil, b, c)
```
You can use ??= with variables to assign a value to it only if its current value is nil:
```ruby
a = nil
a ??= 5 # a is 5
b = 3
b ??= 5 # b is 3
```

#### ? - nil check
Using a method call, getter or setter on a nil results in a nil, which sometimes may lead to bugs or force you to check if the value is nil before invoking any calls on it. To simplify this, you can enforce a nil check using the ? operator. Basically, the ? operator will check if its operand is nil. If it is, it's going to raise a Stdlib NilReferenceException if you attempt to invoke any calls, getters or setters on it. You may think of it in a way that it asserts if a value is not nil before doing any operations with it. Of course, you may always account for that scenario by using a *rescue* block beneath the check.
```ruby
obj = nil
a = obj.property # a = nil, no crash
b = ?obj.property # NilReferenceException is raised
```

#### @ - self referencing
The @ operator maps exactly to *self.*, i.e @tank is identical to writing self.tank. It's primarily there to save time and effort when implementing classes (when you really write a lot of *self.*s).

### !! - get annotations
The !! operator retrieves the list of annotations associated with a field. For more details on usage, check out [Šimi annotations](#annotations).

#### Bitwise operations

All bitwise operations (and, or, xor, unary complement, shift left, shift right, unsigned shift right) are implemented as methods on the Number class:
```ruby
print 12.bitAnd(25) # prints 8
```

### Control flow

#### Truth
When it comes to literal values, determining the truth is easy:
* *nil* is false, and
* 0 (to which the keyword *false* maps to) is false,

while everything else (including empty strings and objects) is true.

#### if-else if-else
Not much to say here, Šimi offers the usual if-else if-...-else -else structure for code branching.
```ruby
if a < b {
  c = d
  print c
} else if a < c {
 print "a < c"
} else if a < d {
  a = e
  print e
} else {
 print f
}
```

#### when
If you're comparing a lot of branches against the same value, it's more convenient to use a *when* statement.
* It supports *is*, *is not*, *in* and *not in* operators; otherwise it assumes that the operator is *==*.
* *if* can be used as an operator to denote that its right-hand side should be evaluated as-is (useful for function calls).
* You can use the *or* operator to check against multiple conditions in a single branch.
* *when* statement can be made exhaustive by adding an *else* block at the end.
```ruby
when a {
    5 {
     print "a is 5"
    }
    10 or 13 or 15 {
     print "a is 10 or 13 or 15"
    }
    is String {
     print "a is String"
    }
    not in Range(12, 16) {
     print "not between 12 and 16"
    }
    if isValidString(a) or is Number {
        print "is valid string or a number"
    }
    else {
     print "reached the default branch"
    }
}
```

Note that *when* is a syntax sugar, i.e it compiles the same way as if-else if-else blocks would.

#### *if* and *when* expressions

*if-else if-else* branches (and, by default, its alternate form of *when*) can be used as expressions. The last line in the block (or the only value if it's a block-less statement) must be an expression, or a return/yield/break/continue - otherwise, a compiler will report an error.

```ruby
a = if someCondition {
    3
} else if otherConditions {
    print "something"
    return "I'm outta this function"
} else if yetMoreConditions {
    print "doing this"
    doThis()
} else {
    2 + 2 * 3
}
```

The *when* equivalent different only as it allows to use *=* for the single-line expression block:

```ruby
a = when b {
    3 or 4 = 10 # See how easy and fast this is to type?
    6 or 7 = 20
    is String = 30
    is List {
        d = a * 3 + 15
        d + 20 # The last line in the block must still be an expression
    }
    else = 50
}
```


Naturally, functions with an implicit return of their single expression work with *if* and *when* expressions as well:

```ruby
def extractValue(obj) = when obj {
    is A = obj.aValue()
    is B or is C = obj.bOrCValue()
    else = obj.otherValue()
}
```

Let's rewrite the *when* example from the previous section as an expression:

```ruby
print when a {
    5 = "a is 5"
    10 or 13 or 15 = "a is 10 or 13 or 15"
    is String = "a is String"
    not in Range(12, 16) = print "not between 12 and 16"
    if isValidString(a) or is Number = "is valid string or a number"
    else = "reached the default branch"
}
```

TODO REMOVE IFE
If you prefer a shorter solution, the Stdlib contains a global function named **ife**, which works exactly as the ternary operator in some other languages (?:) does - if the first parameter is true, the second parameter is returned, otherwise the third parameter is returned. The syntax of the ife function is more readable and forces the user to make use of short, concise expressions for all three parameters.

```ruby
max = ife(a < b, a, b)
```
##### *ife* and lazy loading
If you look at the definition of the *ife* function, you'll see that it has call operator *()* for both of its "branches":
```ruby
def ife(condition, ifval, elseval) = return if condition {
    ifval()
} else {
    elseval()
}
```
In Šimi, you can call any value - it's not just limited to functions and classes. If you invoke a call on a Number, String, nil or non-class Object, it will simply return itself. This allows for lazy loading to happen - if a function allows for it, you can pass params inside parameter-less, single-line lambdas (def = ...), and then those params will be evaluated only when they're used in the said function:
```ruby
step = ife(min < max, 1, -1) # Works with non-function values

# The following example uses functions to lazily compute only the value
# which needs to be returned. Notice "=SOMETHING" syntax, which is shorthand for
# def { return SOMETHING }
step = ife(min < max, =Math.pow(2, 32), =Math.pow(3, 10))
```

#### while loop
The *while* block is executed as long as its condition is true:
```ruby
while a < 10 {
  print a
  a *= 2
}
```

#### do-while loop
The *do-while* loop also executes while its condition is true, but its condition is checked at the end, meaning that it's body is always going to execute at least once.
```ruby
a = 10
do {
  print a # This will still get printed because condition is evaluated at the end
  a *= 2
} while a < 3
```

#### for-in-else loop
Šimi offers a *for-in* loop for looping over iterables (and virtually everything in Šimi is iterable). The first parameter is the value alias, while the second one is an expression that has to evaluate either to an *iterable*. The block of the loop will be executed as long as the iterator supplies non-nil values (see section below).
```ruby
for i in 6 {  # Prints 0 1 2 3 4 5
 print i
}
for c in "abcdef" { # Prints a b c d e f
 print c
}

# Iterating over a list iterates over its values
for value in [10, 20, 30, 40] { # Prints 10 20 30 40
 print value
}

object = [a = 1, b = "str", c = Pen(color = "blue")]
# Iterating over keyed objects over its keys
for key in object {
 print key # Prints a, b, c
}
# Object decomposition syntax can be used with for loop as well
for [key, value] in object.zip() {  # Prints a = 1, b = str, etc.
 print key + " = " + value
}
```

If the expression is *nil*, the loop won't run. However, it it isn't nil and doesn't resolve to an iterator, a runtime error will be thrown.

There are situations in which you want to know that a loop didn't run because its iterator was nil. In such cases, append an *else* block after the for loop:

```ruby
for i in something {
    # do your thing
} else {
    print "something was nil, the loop didn't run!"
}
```

#### Iterables and iterators
The contract for these two interfaces is very simple:
* An *iterable* is an object that has the method *iterate()*, which returns an iterator. By convention, every invocation of that method should produce a new iterator, pointing at the start of the iterable object.
* An *iterator* is an object that has the method *next()*, which returns either a value or nil. If the iterator returns nil, it is considered to have reached its end and that it doesn't other elements.

Virtually everything in Šimi is iterable: TODO revisit
1. The Number class has methods times(), to() and downto(), which return Ranges (which are iterable).
2. The String class exposes a native iterator which goes over the characters of the string.
3. The Object class exposes a native iterator that works returns values for arrays, and keys for objects. There's also a native zip() method, that returns an array of \[key = ..., value = ...] objects for each key and value in the object.
4. Classes may choose to implement their own iterators, as can be seen in the Stdlib Range class.

#### break and continue
The *break* and *continue* keywords work as in other languages - break terminates the active loop, while continue jumps to its top. They must be placed in loops.

#### do-else

A *do* block is a breakable block of code - you can think of it as a mini function that has its own scope, but not its own call frame.
```ruby
do {
    a = 5
    b = 6
    if a + b > 12 {
        break
    }
    print a
}
```

If you follow the *do* block with an *else* block, the code after *else* will be executed *if the do block breaks*. If the *do* block reaches its end normally, the *else* block is discarded.
```ruby
do {
    a = 5
    if a < 3 {
        break
    }
    print "this won't be printed"
} else {
    print "do block exited early"
}
print "continuing normally"
```

It gets better than that - you can use breaks to *pass values* to the else block. Every else block implicitly has a local variable named *it*, which is bound to the value of the break (default is, of course, nil).
```ruby
do {
    a = 5
    if a < 30 {
        break 10
    }
    a += 17
    if a > 3 {
        break 20
    } else {
        break # equal to break nil
    }
    print "this won't be printed"
} else {
    print when it { # "it" is implictly bound and available in the else block
        10 = "ten"
        20 = "twenty"
        else = "something else \(it)"
    }
}
```

The *do-else* construct shines to syphon multiple TODO LINK rescue clauses to the same handler.

#### Procs with do-else

> This language feature is in experimental stage and isn't a part of the final definition.

*do-else* blocks can be used as *procs* - local functions that *aren't closures*, i.e they full share the same scope within which they were declared. You can think of a proc as a zero-argument subroutine - calling it jumps to its start, it does its thing, and when it reaches its end it jumps back to the call site. The important trait of a prop is that, since it is not a closure, it can alter its surrounding scope.

You define a proc by assigning a *do-else* block to a variable. The block(s) must be expression blocks, i.e their last value is taken as the block's return value, just as it did with TODO LINK TO if and when expressions.

Then, you can invoke a proc by calling it as any other function (with zero args, of course).
```ruby
i = 10
p = do { # Defines a proc
    print i
    if i == 10 {
        20
    } else {
        5
    }
}
i $= p() # Call the proc, i will be 20
j = p() # Call the proc, j will be 5
```

Just like a do block, a proc can return a value early with *break*:
```ruby
i = 10
p = do { # Defines a proc
    print i
    if i == 10 {
        break 20
    }
    i += 10
    if i < 30 {
        break 50
    }
    10 # the default return value
}
i $= p() # Call the proc, i will be 20
j = p() # Call the proc, j will be 10
```

A proc can also have an *else* part, in case of which it behaves like a *do-else* block:
1. If the do part reaches its end without breaking, its last line is returned.
2. If the do part breaks, its break value is bound to *it* in the else block, and the last line of the else block is returned.
Note that you **can't return from an *else* block early**!
```ruby
i = 10
p = do {
    if i < 20 {
        break 20
    }
    5
} else {
    25
}
i $= p() + 5 # i will be 30
j = p() # j will be 25
```

#### return and yield

Control flow in functions is done via *return* and *yield* statements.

The *return* statement behaves as it does in other languages - the control immediately returns to whence the function was called, and can either pass or not pass a value:
 ```ruby
 def f(x) {
    if x < 5 {
        return 0
    }
    return x
 }
 print f(2) # 0
 print f(6) # 6
 ```

 A *return* always returns from the current function, be it a lambda or not. Some languages make a distinction there, but Šimi does not.

 The *yield* statement is very similar to *return*, but it's only applicable to [fibers](#fibers). Please check the Fibers section for more info.

### Fibers

Fibers are Šimi **coroutines** - autonomous, suspendable runtimes that can pass data between themselves. You can think of a fiber as a suspendable function, one that can execute itself to a certain point, switch the execution back to its caller, and then *resume where it left off* on the next call. It suspends and resumes can both pass data.

Any Šimi program is always running inside an implicit, main fiber (which you can't yield out of).

Let's illustrate all of this with an example.

The first step towards your first fiber is to write a *fiber class*:
```ruby
fib MyFiber(a, b, c) {
    foreach(a..b, def (i) { # foreach is a function that iterates through first arg and calls second arg with the iteration value
        yield i + c
    })
}
```

The syntax looks awfully like that of functions, because ultimately all a fiber does, code-wise, is wrap a single function (do note the keyword *fib* as opposed to *def*). However, in order to use a Fiber, you have to instantiate it:
```ruby
myFiber = MyFiber()
```

Fibers are always instantiated with a parameter-less call to their fiber class. Trying to pass more than zero arguments here is a runtime error.

After a fiber has been initialized, you can call it just like any other function, by passing the expected number of arguments to it:
```ruby
print myFiber(0, 3, 1)
print myFiber(5, 3, 5)
print myFiber(6, 3, 10)
print myFiber(8, 3, 20)
print myFiber(5, 10, 0)
```

The output of the above is:
```ruby
1
6
12
nil
5
```

Why is that so? Let's examine the execution step-by step:
1. The fiber is invoked with params 0, 3, and 1. This creates a Range(0, 2) and starts iterating through it.
2. *yield* works just like *return*, except the fiber remembers where it left of. This yield return 0 (for i) + 1 (for c) for a sum of 1.
3. The next call kicks off after the last yield, which is still inside the loop, meaning that the changes to a and b don't matter. The change to c does, so the result is 1 (for i) + 5 (for c) which equals 6.
4. It's exactly the same for the next iteration - i = 2 + c = 10 == 12.
5. On the next invocation, the loop ends as we've exhausted the range, and the *implicit return nil* is executed at the end of the function.
6. Since the fiber returned on the previous invocation, it's reset, and the new invocation starts a new iteration through Range(5, 9).

Here are a few takeaway points:
* A *yield* always suspends the nearest fiber, just like return returns from the nearest function. This means that, when a fiber suspends, its entire call stack suspends as well - and resumes where it left off, no matter how many nested function calls it had.
* If a fiber returns, its reset and the next invocation will resume from the start.
* You can pass new arguments to the fiber with each invocation, but it's the structure of the fiber code that determines if they have an effect or not.
* Naturally, you can call fibers from other fibers (remember how your entire program is already in a fiber).

### Exception handling

#### Exceptions
All exceptions in Šimi do (and should) extend the base class *Exception*. The default constructor allows you to define a string message for your exception. Its generally advisable to create your own exception subclass as opposed to discerning exceptions based on their message.

#### Returning exceptions

In Šimi, Exceptions **are returned, and not thrown/raised**. If a certain piece of code wishes to indicate that an exception has occurred, it should return an instance of the Exception class or any of its subclasses. It's a simple as that, and also dead-easy to understand how does control flow in the function where the exception occurred - it immediately returns to its call site.

Just as you probably guessed, this *does not* unwind the call stack in search of the nearest catch block, as there's no such thing in Šimi. Instead, exceptions are handled via **rescue blocks**.

#### Rescue blocks

A rescue block sits at the end of a statement and waits for an exception to be returned from any of the calls or getters. If this happens, it immediately executes the code within its block. After that (or otherwise, if no exceptions are returned in the statement) life goes on as it did before.

A rescue blocks starts with *?!* (you can read this as "if exception"), followed by a block. This is a hard block, and not an expression one, and you can thing of it as sitting *below* the statement it rescues, and not inline with it.

If an exception is returned, it will be bound inside the rescue block as *it*, so that you may access it.

TODO SOME EXAMPLES

If you need a common handler for a lot of rescue blocks, pair it with a do-else, *break it* in all the rescue blocks and see the magic unfold:
```ruby
do {
    someCallThatMightProduceExceptionA() ?! { break it }
    a = thisMight.returnExceptionB + thisMightReturnSomeOtherException() ?! { break it }
} else {
    print when it {
        is ExceptionA = "exception A occurred \(it.message)"
        is ExceptionB = "exception A occurred \(it.message)"
        else = "something else \(it)"
    }
}
```

The *it* in the rescue block (the exception) is transferred via *break* to the *else* block, where it's also bound to the local *it*.

### Enums

Šimi currently doesn't support enums as a first-class language feature, but does bring them in via a pure-Šimi function *Enum.of()*, which is a part of Stdlib. There are no strong reasons or opinions to back this approach: the implementation was coded as an exercise in Šimi metaprogramming, and was found satisfactory and in-line with the general philosophy of reducing cognitive load needed to learn and use the language.
Roughly speaking, the idea behind an enum is to create a custom data type that has a fixed amount of discrete possible values. At the simplest level, *Enum.of()* converts an array of strings into a class that contains like-named intances of that same class. Check out the following example:
```ruby
Fruit = Enum.of(["APPLE", "ORANGE", "BANANA"])
apple = Fruit.APPLE
print "Apple is fruit: " + (apple is Fruit) # true
print "Apple == Apple: " + (apple == Fruit.APPLE) # true
print "Apple == Banana: " + (apple == Fruit.BANANA) # false
```
Most of the time, this is all you need from an enum - a set of possible values that you can simply reference, and can use with *==* and *is* operators. Here, Fruit is a class that extends *Enum* class, and it has three static fields: APPLE, ORANGE, and BANANA, all of which are instances of the Fruit class. Each intance has a field named *value* that contains the string representation of the key (i.e, it's "APPLE" for APPLE).

Instead of using an array, you can pass a key-value object to the *Enum.of()* function to associate data with your enum values:
```ruby
Fruit = Enum.of(["APPLE" = 3, "ORANGE" = 2, "BANANA" = 10])
apple = Fruit.APPLE
print apple.value # 3

Veggies = Enum.of(["POTATO" = [count = 10, location = "cellar"],\
                    "CUCUMBER" = [count = 5, location = "closet"]])
print Veggies.POTATO.count # 10
print Veggies.CUCUMBER.location # closet
```
If the object you passed has non-object keyes, the resulting enum will retain the *value* field. Otherwise, the associated object will be decomposed and its keyes used as fields of the resulting enum class. Note that all the objects that you pass as values need to be of the same type, i.e have the exact same keyes.

Lastly, the *Enum.of()* can take a second parameter, a key-value object containing functions that will be associated with the resulting enum class:
```ruby
Veggies = Enum.of(["POTATO", "CUCUMBER"], [isRound = :self == Veggies.POTATO])
potato = Veggies.POTATO
print "Is potato round: " + potato.isRound() # true
print "Is cucumber round: " + Veggies.CUCUMBER.isRound() # false
```
Overall, that should satisfy most, if not all the needs that a developer has when leveraging enums in their code. If you go as far as creating enums that have both value objects and functions associated with them, the legibility of *Enum.of()* usage starts to deteriorate and would better be served with a dedicated language construct. Also make sure to check out its implementation in Stdlib to learn more on metaprogramming in Šimi!

### The safety package

Šimi is a primarily a dynamically typed language, meaning that its syntax and behavior are tailed towards maximal flexibility at runtime. Examples of this is the ability to assign any value to any variable, pass any value as a parameter, freely call any value or perform gets and invoked on a *nil*.

However, nobody is denying the advantages of strong typing, especially for larger and more serious codebases. Because of this, Šimi includes a set of features dubbed "the safety package", which allow for runtime type and null-checks to make sure that the code does exactly what you want it to under all circumstainces.

#### Argument and return type checks

You can add runtime type checks to function args and return types:
```ruby
def func(a is String, b is Num = 20, c is Range = Range(3, 40)) is Range? {
    ...
}
```

1. You specify the type by putting *is* behind the argument name, followed by the type.
    a. Types are non-nullable by default, i.e "a is String" will fail if *nil* is provided for *a*. To specify that the type is nullable, add *?* at the end of the type: "a is String?".
2. The function return type is checked by putting the *is TYPE* check after the arguments.
    a. The nullability rule applies here as well.
    b. If your function TODO LINK exceptions returns exceptions instead of nil, you can mark the return type as being either it or exception with *!*: "def func() is String!" means that this function should return either a String or an Exception.

Again, these checks are performed at runtime, and will return *TypeMismatchException* if something is off.

#### Nil-safe gets and calls

Šimi permits operations on *nil*, such as getters, setters and calls. All of those return nil and don't produce an exception:
```ruby
a = nil
a.something # nil
a() # nil
```

While being a normal feature of a nullable dynamically-typed language, such code can produce errors that are difficult to trace and fix. To combat this, Šimi has nil-safe gets and calls: *?.* and *?(*. You might've seen this in some statically-typed languages, where they're used on nullable values and return null if the value really is null. In Šimi, it's *the other way around* - normal calls return nil if the value is nil, while safe calls return a *NilReferenceException*:
```ruby
a = nil
a?.something # Safe get, the result is a NilReferenceException instance
a?() # Safe call, the result is a NilReferenceException instance
# You can chain safe and unsafe calls and gets in any order you want
obj.nullableProp?(1, 2, 3).funcThatMayReturnNil("a")?.nullableFunc?()
```

The returned NilReferenceException can be handled with a TODO LINK rescue block, just like any other exception:
```ruby
value = a?.something().that?() ?! {
    return 2
}
```

### Code organization and modularity

It's natural for a growing codebase to be dispersed among multiple files. Similarly, if you're building a library for other people to use, it will come in its own file(s).

#### Importing external code

Šimi allows you to import code from external files with the *external import*:
```ruby
import "Date"
import "../otherlib/Lib"
```

Imports are resolved between lexing and compiling phases - each Šimi code file specified will be lexed and added upstream for compiling, with its external imports resolved recursively. Each file will be imported only once.

Files specified for external import are assumed to be Šimi files (.simi), so you needn't specify the extension. If the full path to the file isn't provided, it's assumed to live in the same directory as the file it's being imported to.

After all the files have been lexed and all the imports resolved, the entire imported codebase will be compiled as a single file.

The *Core* file and its native companion are implicity imported into every Šimi code file, so they don't have to be specified manually.

Native dependencies are imported with *import native* - check out the TODO LINK native API guide for that.

#### Modules

Since the compiler doesn't know which code came from which file, it compiles all of it as a part of a single scope. For larger codebases, especially ones that have a multitude of dependencies, this can lead to name collisions. To solve this, Šimi introduces namespaces via *modules*:
```ruby
module MyModule {
    field = 2 + 2 * 2
    singleton = [name = "This", method = =_0 + _1]

    class Class1
    
    class Class2 {
        ...
    }
    
    def someFunc() {
        ...
    }
    
    fib SomeFib() {
        ...
    }
    
    module Submodule {
        def otherFunc = 5
    }
}
```

Define modules with the *module* keyword. Within their bodies, they can contain declarations of classes, functions, fibers and constants - just like classes can. However, since modules aren't instantiated, their functions and fibers aren't bound, i.e they won't have *self* defined. Also, module constants are assigned at compile-time, unlike class fields which are assigned during instantiation (in the *init* method). Modules can contain other nested modules.

Reference module's field just as you do with classes, using the dot operator:
```ruby
someFib = MyModule.SomeFib()
print MyModuke.Submodule.otherFunc()
```

#### On-site imports

You can import fields of a module directly into any scope by an *on-site import*:
```ruby
a = 5
b = 6
import MyModule for Class1, field
import MyModule.Submodule for otherFunc
print a + b + field
```

On-site import binds the module's fields specified after *for* to their namesake local variables in the current scope. Since Šimi is a dynamic language, and the expression after *import* isn't evaluated at compile-time, importing all the fields from a module isn't possible, instead you have to specify which fields to import.

Since on-site imports can be placed virtually anywhere, they allow you to be judicious with where is a certain name applicable. TODO FIX

### Java API

If you require a functionality that is not readily available in Šimi, and coding it manually might pose a challenge due to complexity or performance issues, you may use the Šimi JVM API and expose "native" JVM code to the Šimi runtime. Let's examine how this works by checking out the Stdlib *Date* class and its *now()* and *format()* methods.

Here's the Šimi implementation:
```ruby
class Date:
    native init
    native at(time = 0)
end
```

There are three levels of Šimi JVM API:
1. A *NativeModule* is instantiated when the JAR is loaded at compile time. It serves only to map *NativeClasses* to their names.
2. A *NativeClass* has to implement a single method which maps a function name to its *NativeFunction* implementation (or null if such a method isn't available).
3. A *NativeFunction* is the native implementation of a Šimi function. It always returns a value (or null), and takes in an array or objects as its arguments. The first argument will always be the native function's receiver - either an instance or an SClass, depending on how the function was invoked.
```kotlin
class Date : NativeModule {
    override val classes: Map<String, NativeClass> = mapOf(
            "Date" to object : NativeClass {
                override fun resolve(funcName: String): NativeFunction? {
                    return when (funcName) {
                        Constants.INIT -> NativeFunction(0) {
                            val instance = it[0] as Instance
                            val date = Date()
                            instance.apply {
                                fields["_"] = date
                                fields["time"] = date.time
                            }
                        }
                        "at" -> NativeFunction(1) {
                            val klass = it[0] as SClass
                            val time = it[1] as Long
                            val date = Date(time)
                            Instance(klass, false).apply {
                                fields["_"] = date
                                fields["time"] = date.time
                            }
                        }
                        else -> null
                    }
                }

            }
    )
}
```
After that, everything's really simple: build the Java project and import its resulting JAR into Šimi code:
```ruby
# These methods are already a part of Stdlib-java.jar, this is just to
# illustrate that you have to import the JAR of your external library.
import "SimiDate.jar"
date = Date(1516004607682) # non-static usage
print date.format("dd/MM/yyyy hh:mm:ss")
now = Date.now() # static usage
print date.timestamp
```
You'll notice that there's no performance degradation associated with using native methods as the code is accessed via reflection only once, when the JAR import is resolved. After that, all native method references are effectively statically typed.

### Annotations

Šimi supports annotations to allow for association of metadata with classes and their fields. This metadata can then be used at runtime by other parts of the code. Annotations start with **!** and come in form of object literals or constructor invocations and precede a declaration:
```ruby
class Api:
    ![method = "GET", endpoint = "/user"]
    def getUser(result)

    ![method = "GET", endpoint = "/appointments"]
    def getAppointments(result)
end
```
You may have as many annotations before a field as you'd like, each in a separate line.

You can get a list of field's annotations using the **!** operator. It returns a list of values based on evaluated annotations. Naturally, if the field has no annotations, this list will be empty. Let's examine a simple class that generates SQL code for creating tables based on a list of model classes.
```ruby
# Model superclass. It doesn't have its own table, but contains a primary key field
# that will be used by all model superclasses.
class Model {
    # The dbField in the annotation specifies that this field will have a column in
    # the corresponding table. If its value is true, we'll infer the column name
    # from the field name. Otherwise, we'll use the supplied string value.
    ![dbField = true, primaryKey = true]
    id = 0
}

# We use the dbTable annotation to indicate that this class should have a table in
# the database. The associated value is the name of the table.
![dbTable = "Users"]
class User(Model) {
    ![dbField = "first_name"]
    firstName = ""

    ![dbField = "last_name"]
    lastName = ""
}

![dbTable = "Appointments"]
class Appointment(Model) {
    ![dbField = true]
    userId = ""

    ![dbField = "timeslot"]
    time = Date()
}

# This class generates SQL code for creating tables based on a list of classes.
class DbLib {
    def init(tables) {
        sqlBuilder = String.builder()
        for table in tables {
            @_sqlForTable(table, sqlBuilder)
        }
        @sql = sqlBuilder.build()
    }

    def _sqlForTable(table, sqlBuilder) {
        classAnnotations = !!table # First we get annotations for the class
        if not classAnnotations {
            return # If there aren't any, skip this class
        }
        for annot in classAnnotations {
            dbTable = annot.dbTable # We're interested in the annotation that has "dbTable" field
            if dbTable {
                name = ife(dbTable is String, dbTable, table.name) # Infer table name
                sqlBuilder.add("CREATE TABLE ").add(name).add(" (\n") # Construct SQL
                @_sqlForFields(table, sqlBuilder) # Add column SQL
                sqlBuilder.add(");\n")
            }
        }
    }

def _sqlForFields(table, sqlBuilder) {
        # Iterate through all the class fields and find those that have an annotation
        # which contains the "dbField" field
        for key in table {
            val = table.(key)
            keyAnnotations = !!val
            if not keyAnnotations {
                continue
            }
            for annot in keyAnnotations {
                dbField = annot.dbField
                if not dbField {
                    continue
                }
                name = ife(dbField is String, dbField, key) # Infer the name
                # Infer type based on calue associated with the field in class definition
                type = when val {
                    is Number: "int"
                    is String: "varchar(255)"
                    is Date: "date"
                    else: nil
                }
                # ... of course, many more types could be added, including relationships to other tables

                sqlBuilder.add(name).add(" ").add(type)
                if annot.primaryKey { # Check for "primary key" field in the annotation
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
}

(def {
    db = DbLib([Model, User, Appointment])
    print db.sql
})()
```
The output of this code is:
```
CREATE TABLE Users (
first_name varchar(255),
last_name varchar(255),
id int NOT NULL AUTO_INCREMENT,
PRIMARY KEY (id),
);
CREATE TABLE Appointments (
timeslot date,
id int NOT NULL AUTO_INCREMENT,
PRIMARY KEY (id),
userId varchar(255),
);
```

### Metaprogramming and (de)serialization - *gu* and *ivic*

While designing Šimi, a deliberate attempt was made to blur the line between the code and interpreter's inner workings. If the interpreter's input is code, then it should also be able to produce valid Šimi code as its output, which can then be taken up again by an interpreter. Two unary operators, *gu* and *ivic*, make this incredibly easy to accomplish, and their interoperability allow for seamless and interesting approaches at metaprogramming and (de)serialization.
The basics:
* **gu** takes a string (or an expression that evaluates to string) representation of a Šimi statement as input and interprets it, returning the result of the statement. E.g, gu "2+2" == 4
* **ivic** takes a Šimi value as input and dumps it to a Šimi code string that's readily interpetable. Any output produced by the *ivic* operator can be readily pasted in Šimi code or invoked by the *gu* operator.

An obvious power of this combo is that any internal state of a Šimi interpreter can be dumped into code and then interpreted again, making serialization and deserialization of Šimi classes, objects, or any other piece of code trivial. This is similar to what JSON does for JavaScript, with the added benefit of being able to serialize *anything* - functions, classes, annotations, etc:
```ruby
class Car {

    wheels = 4

    def init(capacity, tank): pass

    def refill(amount) {
        if amount < 0 {
            CarException("Amount < 0!").raise()
        }
        if amount > 100 {
            TankOverflowException("Too much gasoline!").raise()
        }
        if tank + amount > capacity {
            @tank = capacity
        } else {
            @tank = tank + amount
        }
    }

    def refill(amount, doprint) {
        tank = 0
     }
}

carInstance = Car(40, 10)

print ivic Car # printing the class
print ivic carInstance
```
The code above produces the following output:
```
class Car {
    wheels = 4
    def init(capacity, tank) {
        self.capacity = capacity
        self.tank = tank
    }
    def refill(amount) {
        print "Doing something"
        if amount < 0 {
            CarException("Amount < 0!").raise()
        }
        if amount > 100 {
            TankOverflowException("Too much gasoline!").raise()
        }
        if tank + amount > capacity {
            self.tank = capacity
        }
        else {
            self.tank = tank + amount
        }
    }
    def refill(amount, doprint) {
        self.tank = 0
    }
    def f() {
        return pass
    }
}
[
    "class" = gu "Car",
    capacity = 40,
    tank = 0
]
```
As you can see, the entire code of the Car class has been printed, and the instance object was also dumped in an interpretable format.
Naturally, invoking *gu ivic* clones objects:
```ruby
obj = [a = 5,
    b = "aaa",
    c = def (a, b) {
        result = a + b + "dddd"
        return result
    }
]
clone = gu ivic obj
print clone <> obj # true
```

These operators also allow for some interesting approaches to metaprogramming. Take, for example, how *gu* is used in *Enum.of()* function to generate the enum class and associate values with it:
```ruby
guStr = "class " + className + "(Enum) {
    def init(" + args + "): pass
    def equals(other): return @matches(other)
}"
clazz = gu guStr

...

for key in obj {
    val = if isArray {
        clazz(key, key)
    } elsif isFirstValueScalar {
        clazz(obj.(key), key)
    } else {
        args = String.from(obj.(key).values(), ", ")
        args += ", \(key)"
        constructor =  "clazz(" + args + ")"
        gu constructor
    }
    clazz.(key) = val
}
```
Combining this with *ivic* allows for creation of programs that *change their code on the fly*. Simply, dump a block/function/method to code with *ivic*, use string manipulation to alter its body, and use it again by supplying the altered code to *gu*. Check out this [simple genetic algorithm](https://github.com/globulus/simi/blob/develop/genetic.simi) to see the duo in action!


### Debugger

Šimi has a limited built-in debugger that can be used to place breakpoints and examine environment when they trigger. The debugger is autoenabled for CLI Šimi invocations (using simi.jar), and can be used with ActiveSimi through *setDebugMode(true)*. Using a debugger adds a small overhead in both speed and memory.

#### Breakpoints

To set a breakpoint at a given line, add a comment at its end that starts with **BP**:
```ruby
a = 5
b = 6 # BP (this will trigger a breakpoint)
c = 7
```
As the program runs and encounters a breakpoint or a fatal exception (where your program would normally crash), it will pause and print out:
1. a stack trace 20 frames deep,
2. environment for the first frame, not including the global environment.

When a breakpoint or a crash triggers, you can type in the following commands to use the debugger:
* *c* - prints the call stack.
* *l* - prints the line stack (lines executed prior to reaching the current line).
* *i \[index]* - prints the environment for the frame at given index.
* *e \[expr]* - evaluates the provided expression in the current environment and prints out the result.
* *w \[name]* - adds variable in the current environment to Watch.
* *n* - step into - trigger breakpoint at next line.
* *v* - step over - trigger breakpoint at next line skipping calls.
* *a* - adds current line at breakpoint for this debugger session. To be used in conjuction with "step into" and "step over".
* *r* - removes the current breakpoint. You cannot alter breakpoints at runtime as they are gathered from comments during the scanning phase, but you can choose to ignore some in your current debugging session by using the *r* command.
* *x* - toggles catching all exceptions on/off (default off). Normally, the debugger will only trigger if a fatal exception is encountered, but can be configured to catch all exceptions so that you can inspect the environment capture more thoroughly.
* *o* - toggles debugging on/off (default on). Breakpoints and exceptions will not trigger if debugging is off.
* *h* - prints help.
* *g* - prints out global environment.

Typing in anything else (or a newline) will resume the execution of the program.


#### Visual debugging

Šimi Debugger's interface is decoupled from its functionality, allowing your to create a visual debugger on any platform supporting Šimi. At its most basic level, debugger interface allows for sync and async exchange of textual commands and responses between the debugger and visual interface. On top of this, *ActiveSimi* can expose the debugger watcher that allows for querying of specific debugger data structures, allowing for different debugger components (environment, watches, breakpoints) to be rendered in separate UI views.

ŠimiSync's [web part uses a browser interface for debugging a Šimi server](https://github.com/globulus/simi-sync/blob/master/web/src/main/java/net/globulus/simisync/BrowserInterface.java), while [Android](https://github.com/globulus/simi-sync/tree/master/android/AndroidDebugger/src/main/java/net/globulus/simi/android/debugger) and [iOS](https://github.com/globulus/simi-ios/tree/master/SimiLib/iOSSimi/iOSSimi) use mobile screens that show up once debugger triggers a breakpoint or an exception.

### Basic modules

Šimi's Stdlib comes built in with a number of modules that can be used to accomplish various tasks. Below is a quick outline of most of them:

#### Stdlib

The [Stdlib file](stdlib/Stdlib.simi) is a core module that is inseparable from the Šimi interpreter. It defines basic Šimi classes and functionality in the Šimi language, and bridges some of their functionality to native interpreter classes and methods. On JVM systems, a small portion of the file is backed by a native JAR *Stdlib-java*.

*Stdlib* contains the following core classes:
* Object - defines Objects and all of their methods.
* Function - defines Functions but doesn't contain any methods.
* Number - defines the Number type and associated methods.
* String - defines the String type and associated methods.
* Exception - all Šimi errors and exceptions are wrapped in this class. Stdlib also defines a number of common exception types, namely:
    + ScannerException - raised during scanning/lexing phase of interpreting Šimi code.
    + ParserException - raised during parsing of scanned tokens phase of interpreting Šimi code.
    + InterpreterException - raised by the interpreter itself as encounters an error during interpreting.
    + NumberFormatException - raised, amongst other things, when a String cannot be converted using *toNumber()* method.
    + NilReferenceException - raised when a *nil* is encountered when [nil checking operator](#---nil-check) is used.
    + IllegalArgumentException - can be raised while checking params of a method.
    + AbstractMethodException - can be put into bodies of methods that are meant to be overridden.
    + TypeMismatchException - raised when checked params are used and don't match.
* Iterator - defines iterable behavior.
* Closable - defines closable behavior.
* Range - commonly used class that defines a range of numbers.
* Enum - see [enums](#enums).
* Date - simple timestamp wrapper backed by a native class.
* Debugger - allows for native static fetching of *DebuggerWatcher* instance, allowing for debugging info to be read from Šimi code. See [Šimi Sync web browser interface's SMT template file for usage](https://github.com/globulus/simi-sync/blob/master/web/src/main/resources/static/simi_debug.html).

Stdlib also contains the *ife* method and *Math* object.

#### File

The [Simi File module](stdlib/File.simi) provides an OS-agnostic interface for working with files. The main class, *File*, is at its most basic level a wrapper around a String denoting a path to a certain file, and provides a number of instance methods that manipulate the file name, as well as a single native method, *isDirectory*. The class also contains a number of static native methods.

Files can be created, written and read using native classes *ReadStream* and *WriteStream*. Both of these classes are closable and should be released manually after usage to avoid leaks. Generally, all file operations should be backed by a *rescue* block as they may throw *IoException*.

Here is a simple code that demonstrates reading and writing of files:
```ruby
def testFile() {
    file = File("README.md")
    reader = ReadStream(file)
    while true {
        line = reader.readLine()
        if line {
            print line
        } else {
            break
        }
    }
    writer = WriteStream(File("writetest.txt"))
    writer.write("string")
    writer.newLine()
    writer.write("new string")
    rescue ex { }
    reader.close()
    writer.close()
end
```

#### Net

The [Simi Net module](stdlib/Net.simi) is meant to provide easy access to basic HTTP calls, backed by a native implementation ([Java](https://hc.apache.org/httpcomponents-client-ga/quickstart.html) and [Android](http://hc.apache.org/httpcomponents-client-4.3.x/android-port.html) are backed by their Apache HttpClient libs, while iOS is backed by [AFNetworking](https://github.com/AFNetworking/AFNetworking)).

All methods (get, post, put, and delete) take a request object as a parameter, which should contain a url, headers, and an optional body. All methods take a single callback, so they're well-suited for usage with [async yield](#async-programming-with-yield-expressions)).

Check out [SimiSync-generated Client Tasks code](https://github.com/globulus/simi-sync/tree/master/web#client-tasks) to see the Net module in action!

#### Test

The [Simi Test module](stdlib/Test.simi) module provides capability to write unit tests with mocks. Write a test class, annotate its methods as test cases, add mocks where necessary, and invoke the *Test.test()* method on classes you wish to test. *Test.Report* is a convenience class that translates the test results object into a textual summary.

The Test class contains several annotations that are used to set the test suite up:
* *Before* - method with this annotation will be invoked before every test case.
* *After* - method with this annotation will be invoked after every test case.
* *Case* - denotes a test case method that will be invoked and its success or failure reported.
* *Mock* - allows for mocking fields that are used in test cases. Takes a single parameter, an object whose key/value pairs denote which fields to replace with mocked values. Mocked values can be objects, classes, methods, or anything else that matches the signature of the field being mocked. Mocking can be used on *both* the test class as well any of its methods - class mocks are executed with every test case (and are valid for Before and After methods), whereas method mocks are only valid for the annotated method (Case, Before or After).

Each test case is carried out in a separate environment, invoked on a fresh instance of the test class. The test class will be instantiated using a *parameter-less constructor*, so if you need to perform any initialization, do it in there. Also, all the invoked methods (mocks, before, after, and cases) are invoked with [environment invocation]((#closure-vs-environment-invocation)), so you may want to use this invocation inside test cases as well in order to make sure mocks will work.

Tests should generally be used in conjunction with the *Assert* singleton, which provides several methods for asserting conditions, and each raises a descriptive *AssertionException* when failing.

Here is a simple example that contains two test classes, and prints their report:
```ruby
import Test
class TestCase {
    !Before()
    def before {
        print "before"
    }

    !After()
    def after {
        print "after"
    }

    !Case()
    def testEq {
        Assert.equals(5, 5, "Should pass")
    }

    !Case()
    def testEqFail {
        Assert.equals(5, 4, "Should fail")
    }

    !Case()
    def testInterpreterFail {
        c = "b" - "a"
        Assert.equals(5, 4, "Shouldn't happen")
    }
}

!Mock([Net = [post = def (args, callback): callback("posted")]])
class TestMock {
    !Case()
    def testPost() {
        result = yield Net.post([])
        Assert.equals(result, "posted", "Should be good")
    }

    !Mock([File = [readString = :"mock reading"]]) # Mock must be above to prevent annotations from applying to mock obj
    !Case()
    def testPostAndFile() {
        result = yield Net.post([])
        Assert.isTrue(result == "posted", "Should be good")
        fileString = File.readString()
        Assert.equals(fileString, "mock reading", "Also good")
    }

    !Case()
    def failWithoutFileMock() {
        fileString = File.readString()
        Assert.equals(fileString, "mock reading", "Will fail, File not mocked")
    }
}
print Reporter.report(Test.test([TestCase, TestMock]))

# Output is:
#
# before
# after
# before
# before
# Total: 3 / 6 (50.0%)
#
# TestMock 2 / 3 (66.7%)
# testPost: PASS
# testPostAndFile: PASS
# failWithoutFileMock: Assertion failed at "Equals": "Will fail, File not mocked", params: [10, "mock reading"]
#
# TestCase 1 / 3 (33.3%)
# testEqFail: Assertion failed at "Equals": "Should fail", params: [5, 4]
# testEq: PASS
# testInterpreterFail: ["Simi" line 715] Error at '-': Operands must be numbers.
```

#### SMT

SMT stands for ŠimiText, and is a method of embedding Šimi into text files, so that their content may be generated at runtime using the Šimi interpreter. Its purpose and usage makes it similar to [ERB](https://www.stuartellis.name/articles/erb/) or [JSX](https://reactjs.org/docs/introducing-jsx.html).

Stdlib class named [Smt](stdlib/Smt.simi) does all the magic. To use SMT, first invoke a static method *compile*, supplying a template which is an SMT string. This will return an Smt class instance, on which you can then invoke method *run*, which will interpret all the previously parsed Šimi code in the current environment, and return the resulting string. (Note that you may have to use [environment invocation]((#closure-vs-environment-invocation)) with *run*.)

Consider the following SMT text file that represents HTML code interspersed with Šimi:
```html
<html>
    <body>
        <%! docWideVal = 5 %>
        Value is: <b><%= value %></b> (should be 3) <%# This is a comment %>
        <ul>
            %%for i in docWideVal.times() {
                <li>Loop value is <%= i %>%_
                %%if i % 2 {
                    %_ odd%_
                %%}
                %%else {
                    %_ even%_
                %%}
                %_ number</li>
            %%}
        </ul>
    <body>
</html>

```
Here are the rules for injecting Šimi code into any text:
* To insert a textual result of a Šimi expression, use **<%= EXPRESSION %>**.
* To insert a discardable comment anywhere in the text, use **<%# COMMENT %>**.
* Declare a document-wide variable using **<%! VAR_NAME = EXPRESSION %>**
* **%%** denotes that this line contains a Šimi statement, such  as a loop or a branching. The entire line is considered to be the statement, and no text can appear in it. Every statement must have an accompanying **%%end**, meaning that one-line bodies are not allowed. The body of the statement can be anything - more statements, text, or expression injections.
* **%_** denotes that whitespace should be ommited - when placed at the start of a line, it'll ignore the whitespace before it. When at the end of a line, it will not insert a newline (normally, newlines are rendered as found in the template).

Running the above template with the following code:
```ruby
import "./Stdlib/File.simi"
import "./Stdlib/Smt.simi"

value = 3
template = File.readString("testSmt.html.smt")
smt = Smt.parse(template)
result = smt.run()
print result
```
will produce the following output:
```html
<html>
    <body>
        Value is: <b>3</b> (should be 3)
        <ul>
                <li>Loop value is 0 even number</li>
                <li>Loop value is 1 odd number</li>
                <li>Loop value is 2 even number</li>
                <li>Loop value is 3 odd number</li>
                <li>Loop value is 4 even number</li>

        </ul>
    <body>
</html>
```
Smt is heavily used with [Šimi servers](https://github.com/globulus/simi-sync/tree/master/web) to render views.

#### SQL and ORM

Files located in [stdlib/sql](stdlib/sql/) serve as an interface for connecting to relational databases. Currently, [MariaDB](https://mariadb.com/) is used to illustrate how to natively connect to a DB and map its result into Šimi Db and ResultSet classes.

The [Orm](stdlib/sql/Orm.simi) exposes Object-Relational Mapping that can be used to easily map Šimi objects into DB table rows. To start, annotate a class with **Orm.Table**. Then, use **Orm.Column** annotation on its fields to declare the table colums. Names, nullability and data types can be inferred, or stated explicitly. **Orm.PrimaryKey** annotation is used in addition to Orm.Column to denote the primary key column. To boot your Orm instance, invoke the *createTable* method with all the table classes you wish to support, and it will sert everything up for you. From there on, you can perform selection, insertion, update and deletion by using fluent syntax that works with the annotated classes instead of plain objects.

Check out ŠimiSync's [DbHelper](https://github.com/globulus/simi-sync/blob/master/web/src/main/resources/static/db/DbHelper.simi) and [BeerController](https://github.com/globulus/simi-sync/blob/master/web/src/main/resources/static/controllers/BeerController.simi) classes to see the Orm action on a Šimi backend!

#### CodeBlocks

The [CodeBlocks](stdlib/CodeBlocks.simi) module is meant for easy (de)composition of Šimi code in order to manipulate it and facilitate its usage with [*gu* and *ivic*](#metaprogramming-and-deserialization---gu-and-ivic). Currently it holds the following classes:
* **ClassCode** - converts a Class into code, allowing you to extract its name and body.
* **ClassComposer** - allows for easy, fluent composition of a Class, starting with a name, and then adding fields, methods, or other code. You can extract the code with *getString* or the class with *getClass*.
* **FunctionComposer** - allows for Function composition, starting either from name and params, or another base function. Allows for adding line functions, and extracting the code with *getString* or function itself with *getFunction*.
* **FunctionCode** - converts a Function into code, allowing you to extract its name, parameters, arity, declaration, anonymous declaration, and body.

Check out ŠimiSync's [SimiSyncControllers class](https://github.com/globulus/simi-sync/blob/master/web/src/main/resources/static/SimiSyncControllers.simi) as it uses CodeBlocks extensively to precompute controllers and their rendering!

### Android integration

You can use Šimi in your Android app. Check out [our other project](https://github.com/globulus/simi-android)!

### iOS integration

You can use Šimi in your iOS app. Check out [our other project](https://github.com/globulus/simi-ios)!

### To-Dos

Here's a list of features that might make it into the language at some point in the future:
1. **Decorator annotations**: if an annotation supplied to a function is a function, the annotation function would be executed as a wrapper for the annotated function whenever the latter is invoked. This would allow for some and concise code, e.g when coding a networking API, in which you'd decorate your functions with networking library wrappers, which would then be invoked whenever your functions are. I unsure about this one as it would make the annotation part of function definition, and the resulting confusion might prove to be a large drawback.
**This is currently supported via the [Decorator class](stdlib/Decorator.simi).**

### Keyword glossary

Below is a glossary that lists all the Šimi keyword and their uses. It can be used as a quick reference when reading Šimi code without having to study the entire documentation:
* *and* - used as a [logical operator](#logical).
* *break* - immediately [terminates the loop](#break-and-continue) inside which it is nested. Try to break outside of a loop throws an *InterpreterException*.
* *class, class$, class_* - defines [a (regular, open or final) class](#classes).
    + To get a value's class, just use *class*: obj.class
* *continue* - proceeds to the [next iteration of the loop](#break-and-continue) inside which it is nested. Try to continue outside of a loop throws an *InterpreterException*.
* *def* - defines [a function](#functions). Function declaration is very consistent in Šimi, and all functions, regardless of their purpose (regular functions, lambdas, methods, constructors) are always prefixed by *def*.
    + A parameterless, single-line lambda can be written with just the colon: length = :@len()
* *else* - used as the default clause in [if-elsif-else](#if-elsif-else) and [when](#when) statements.
* *elsif* - defines an alternative clause in an [if-elsif-else](#if-elsif-else) statement.
    + The misspelling is intentional.
* *false* - the [false value](#truth), equivalent to a Number with value 0.
* *for* - defines [a for loop](#for-in-loop), which is used to iterate over [iterators and iterables](#iterators-and-iterables).
* *gu* - unary operator that [evaluates the String supplied to it](#metaprogramming-and-deserialization---gu-and-ivic), allowing for Šimi code to be executed at runtime.
* *if* - defines [an if statement](#if-elsif-else).
* *in*
    + Shorthand for method *has*. [Default behaviour](#in-and-not-in) includes checking presence of substrings, and keys or values in objects.
        - Can be used in [when statements](#when).
    + As a part of [for loop syntax](#for-in-loop).
    * [Defining mixins](#classes).
* *import*
    * Imports [external code dependencies](#importing-code).
    * Static import of Class fields when it is [used as a module](#using-classes-as-modules).
* *is*
    + Checks if the left-hand argument [is an instance](#is-and-is-not) of a right-hand argument, which must be a Class.
        - Can be used in [when statements](#when).
    + Used in static type checking of [function parameters](#functions).
* *ivic* - unary operator that [returns the Šimi code of a value supplied to it](#metaprogramming-and-deserialization---gu-and-ivic), allowing for Šimi programs to codify themselves at runtime.
* *nil* - the special "absence of value" [value](#values).
    + All Šimi fields are nullable, and calling a nil, or inoking getters or setters on it, will result in a nil.
* *native* - denotes that a [function is implemented natively](#functions), i.e in a language other than Šimi (Java for JVM systems and Objective-C or Swift for iOS/OSX).
* *not*
    + As a [unary operator](#logical), logically inverts a value.
    + Is used in [is not](#is-and-is-not) and [not in](#in-and-not-in) operators for improved legibility.
* *or* - used as a [logical operator](#logical).
    + Has special meaning in [when statements](#when).
* *pass*
    + Denotes an [empty statement](#pass) in single-line blocks that don't do anything. As such, it is completely equivalent to a block where a colon is followed by an *end* in a new line.
    + The developer passes the responsibility of generating some code to the parser or interpreter:
        - Constructors with a single pass will [autoset their params to instance vars](#classes).
        - Setter methods with a single pass will [generate setter code based on method name](#classes).
* *print* - invokes the *toString* method on the supplied value and prints the resulting String to Stdout.
    + May be removed in the future should this functionality be moved to the *Io* object in *Stdlib*.
* *rescue* - defines [a rescue block](#the-rescue-block), the centerpiece of Šimi [exception handling](#exception-handling).
* *return* - immediately [returns from the current function](#return-and-yield). It may or may not return a value.
* *self* - references the [object in which the current environment](#classes) is running. Is null if code is being executed in functions not bound to an object.
    + Special *self(def)* construct references the function being exectued.
* *super* - references the [superclass of the object](#classes) in which the current environment is running. Since Šimi allows multiple inheritance, you can specify which superclass to reference via *super(SuperClassName)* construct.
* *true* - the [true value](#truth), equivalent to a Number with value 1.
* *when* - defines [a when statement](#when).
* *while* - defines [a while loop](#while-loop).
* *yield*
    * Used as a statement, it returns from the current function, but remembers where it left off, so subsequent invocations of the same function will resume after the yield. [See examples for more details.](#return-and-yield)
    * Used as an expression, it allows for flatter code by [hiding callback invocations of async functions](#async-programming-with-yield-expressions).
