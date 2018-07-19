# Šimi - an awesome programming language
Šimi (*she-me*) is small, object-oriented programming language that aims to combine the best features of Python, Ruby, JavaScript and Swift into a concise, expressive and highly regular syntax. Šimi's interpreted nature and built-in metaprogramming operators allow the code to be updated at runtime and features to be added to the language by anyone!

You can run Šimi on any machine that has JVM by invoking the Simi JAR, which involves virtually all servers and desktop computers. There's also native support for devices running [Android](https://github.com/globulus/simi-android) or [iOS](https://github.com/globulus/simi-ios).

What Šimi offers:
* Modern, powerful and expressive syntax.
* Highly regular grammar - reduce cognitive load.
* Full OOP support.
* [Metaprogramming operators](#metaprogramming-and-deserialization---gu-and-ivic) allows for language extensions and syntax sugar to be added by anyone.
* Rich [Java API](#java-api) - if it exist in Java, you can easily bridge it into Šimi!
* Run Šimi on any device that supports JRE, with special support for Android and iOS.
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
      - [Objects](#objects)
        * [Objects vs Arrays](#objects-vs-arrays)
      - [Value conversions](#value-conversions)
    + [Classes](#classes)
    + [Operators](#operators)
      - [Arithmetic](#arithmetic)
      - [Assignment](#assignment)
      - [Logical](#logical)
      - [Comparison](#comparison)
      - [is and is not](#is-and-is-not)
      - [in and not in](#in-and-not-in)
      - [?? - nil coalescence](#---nil-coalescence)
      - [? - nil silencing](#---nil-silencing)
      - [@ - self referencing](#---self-referencing)
    + [Control flow](#control-flow)
      - [Truth](#truth)
      - [if-elsif-else](#if-elsif-else)
      - [when](#when)
      - [The *ife* function](#the-ife-function)
      - [while loop](#while-loop)
      - [for-in loop](#for-in-loop)
      - [Iterators and iterables](#iterators-and-iterables)
      - [break and continue](#break-and-continue)
      - [return and yield](#return-and-yield)
    + [Exception handling](#exception-handling)
      - [Exceptions](#exceptions)
      - [The *rescue* block](#the-rescue-block)
    + [Enums](#enums)
    + [Importing code](#importing-code)
    + [Java API](#java-api)
    + [Annotations](#annotations)
    + [Metaprogramming and (de)serialization - *gu* and *ivic*](#metaprogramming-and-deserialization---gu-and-ivic)
    + [Android integration](#android-integration)
    + [iOS integration](#ios-integration)
    + [To-Dos](#to-dos)

### Basic syntax

#### Keywords
Šimi has 27 reserved keywords:
```ruby
and break class continue def else elsif end false for gu if import
in is native nil or pass print rescue return self super true while yield
```

#### Comments
```ruby
# This is a single line comment
print b # They span until the end of the line
```
Šimi currently does not support multiline comments.

#### Identifiers
Identifiers start with a letter, _ or $, and then may contain any combination of letters, numbers, underscores or dollar signs. Identifiers are case sensitive. Note that identifiers that start with _ or $ have special meanings in some instances.

#### Newlines
Line breaks separate Šimi statements and as such are meaningful. E.g, a block that has a newline after ":" must be completed with an *end*. Similarly, an assignment statement must be terminated with a newline. If your line is very long, you can break it into multiple lines by ending each line with a backslash (\\) for readability purposes.
```ruby
a = 5 # This is a line
# Here are two lines, the first one nicely formatted by using \
arr = [1, 2, 3]\
    .reversed()\
    .joined(with = [5, 6, 7])\
    .filter(def i: i <= 5)\
    .map(def i: i * 2)\
    .sorted(def (l, r): -(l <> r))
print arr
```

#### pass
The *pass* keyword is used to denote an empty expression, and is put where a statement or expression is syntactically required, but nothing should actually happen.
```ruby
# It can also be used for specifying empty methods that are meant to be overriden
def next(): pass
```

#### Code blocks
A part of Šimi's consistent syntax is reflected in its use of code blocks - every single block of code starts with a colon (:), followed by one or multiple statements. If the block has only one statement, it can be placed in the same line as the block, otherwise the statements must start in a new line, and the block needs to be termined with an *end*.
```ruby
if a < 5: print a # Single line block
else: # Multiline block, termined with an end
    a += 3
    print b
end
```
Blocks are used everywhere with the exact same syntax, for classes, functions, lambdas, and control flow statements.

#### Variables and constants
In Šimi, you needn't declare the variable before using it, a simple assignment expression both declares and defines a variable.

By default, all variables in Šimi are constants, i.e their value can't change once it's been assigned (and it's assigned right at the declaration). If you want a real variable, its name must start with a dollar sign ($). This makes sure that, at all times, you know that the value of a variable you're accessing could've been changed somewhere in the code, which is not the case with constants.
```ruby
# Here are some constants
a = 5
b = "str"
c = [name = "Peter", surname = "Parker"]

# Here are some variables
$d = 5
while $d < 15: $d *= 2
$e = "another string"
```

### Values

Šimi supports exactly 5 values, making it extremely easy to infer types and know how does a certain part of code work:
1. Number - equivalent to a *double* in other languages, represents 64-bit floating-point number. Also serves as the boolean type, with *true* mapping to 1, and *false* mapping to 0.
2. String - represents a (multiline) piece of text, enclosed in either single or double quotes.
3. Function - a parametrized block of code that can be invoked. Functions are first-class citizens in Šimi and can be passed around as normal values.
4. Object - the most powerful Šimi construct, represents a mutable or immutable array/list, set, dictionary/map/hash, or a class instance.
5. Nil - represents an absence of value. Invoking operations on nils generally results in a NilPointerException.

#### Numbers
Šimi doesn't make a distinction between integers and floating point numbers, but instead has only a double-precision floating point type, which doubles as a boolean type as 0 represents a false value.

When numbers are used as objects, they're boxed into an instance of open Stdlib class Number, which contains useful methods for converting numbers to strings, rounding, etc.

Numbers are pass-by-value, and boxed numbers are pass-by-copy.

#### Strings
Strings are arrays of characters. They can be enclosed either in single or double quotes. You can freely put any characters inside a string, including newlines. Regular escape characters can be included.
```ruby
string = "this is a
    multiline
 string with tabs"
anotherString = 'this is another "string"'
```
When used as objects, strings are boxed into an instance of open Stdlib class String, which contains useful methods for string manipulation. Since strings are immutable, all of these methods return a new string.

Strings are pass-by-value, and boxed strings are pass-by-copy.

The String class contains a native *builder()* method that allows you to concatenate a large number of string components without sacrificing the performance that results from copying a lot of strings:
```ruby
class Range:

    ... rest of implementation omitted ...

    def toString():
        return String.builder()\
            .add("Range from ").add(@start)\
            .add(" to ").add(@stop)\
            .add(" by ").add(@step)\
            .build()
    end
end
```

##### Boxed numbers and strings
Boxed numbers and strings are objects with two fields, a class being Number of String, respectively, and a private field "_" that represents the raw value. The raw value can only be accessed by methods and functions that extend the Stdlib classes, and is read-only. Using the raw value alongside the @ operator results in the so-called *snail* lexeme:
```ruby
# Implementation of times() method from Number class
def times(): return ife(@_ < 0, Range(@_, 0), Range(0, @_)).iterate()
```

#### Functions
A function is a named or unnamed parametrized block of code. Here are valid examples of functions:
```ruby
# A free-standing function with two params
def add(a, b):
    c = a + b
    return c
end

# A function stored in a variable, equivalent to the example above
subtract = def (a, b):
    c = a - b
    return c
end

# Functions that map to native code have a single native statement
# in their bodies
def pow(a, b): native

# Functions without parameters are required to have parentheses
def printFunction(): print "printing"

# A lambda function passed to filter an array, has implicit return if it's single line
# Lambdas with a single parameter needn't put the parameter in parentheses
filteredArray = [1, 2, 3, 4, 5].filter(def i: i > 2)
```
Functions start with the *def* keyword. Native functions are required to have a single *native* statement in their bodies.

Functions in class bodies are called *methods* and have a few caveats to them, which we'll cover in the Classes section.

Functions can be invoked by using (), and passing arguments in them. You can *name* any parameter in the function by putting a name and an equals sign before the value. This helps readability, and the Šimi interpreter will disregard anything up to and including the equals sign for a given parameter.
```ruby
sum = add(3, 4)
difference = subtract(a = 5, b = pow(2, 3))
printFunction()
```

For functions that take no arguments or more than 1 argument, you can invoke them by passing an object whose size is the same as the number of expected arguments. The object will be decomposed by the interpreter, and its values passed as function parameters. This allows both functions and their invocations to be fully dynamic:
```ruby
def f(a, b, c): pass
f(1, 2, 3) # The conventional way of doing it
f([4, 5, 6]) # Invoking with an array
f([a = 7, b = 8, c = 9]) # Invoking with dictionary
# All these invocations do the same thing!
```

You can check if a value is a function by testing agains the *Function* class:
```ruby
fun is Function
```

#### Objects
Objects are an extremely powerful construct that combine expressiveness and flexibility. At the most basic level, every object is a collection of key-value pairs, where key is a string, and value can be any of 4 Šimi values outlined above. **Nils are not permitted as object values, and associating a nil with a key will delete that key from the object.** This allows the Objects to serve any of the purposes that are, in other languages, split between:
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
    add = def (a, b): return a + b
    subtract = def (a, b): return a - b
]
```
Object literals are enclosed in brackets (\[ ]), with mutable object having a $ before the brackets ($\[ ]). An empty mutable object can become either an array, based on the first operation that's done to it - if it's an add/push or addAll(array), it will become an array, otherwise it's considered to be a dictionary.

Getting and setting values is done via the dot operator (.). For arrays, the supplied key must be a number, whereas for objects with keys it can either be an identifier, a string or a number.
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
print a # 3
print b # 4
print d # nil
```

##### Objects vs Arrays
Šimi Arrays are Objects, insofar as they inherit the Object class, and are exposed via the SimiObject interface in the API. That being said, you cannot really mix arrays and keyed objects together, i.e you can't invoke an addAll method on an array with an object parameter, and vice-versa. The reasons for that are twofold, the first being that such operations don't really make sense and their outcome would need to be based on a contract, which would unnecessarily complicate the language. The second reason is performance - if arrays and keyed objects were implemented the same way in the interpreter, the execution time of list operations such as insertion would be much worse.

#### Value conversions
* Numbers can be converted to Strings via the toString() method.
* Conversely, Strings can be converted to Numbers by using the toNumber() method, but be aware that invoking this method may produce NumberFormatException that needs to be handled via the *rescue* block.
* Objects (and all their derivations) have a toString() method that prints a human-readable representation of the object.
* If you wish to have a custom representation of a class, override the toString() method in it.

### Classes
Šimi is a fully object-oriented languages, with classes being used to define reusable object templates. A class consists of a name, list of superclasses, and a body that contains constants and methods:
```ruby
class Car(Vehicle):
    wheels = 4

    def init(brand, model, year): pass

    def refuel(amount): @fuel = Math.min(@tank, @fuel + amount)

    def drive(distance):
        fuelSpent = @expenditure * distance
        @fuel = Math.max(0, @fuel - fuelSpent)
    end
end

car = Car("Peugeot", "2008", 2014) # Creating a new class instance
print car.brand # Peugeot
```
Here's a quick rundown of classes:
* By convention, class names are Capitalized.
* Šimi supports multiple inheritance, i.e a class can have any number of Superclasses. This is partly because Šimi doesn't make a distinction between regular classes, abstract classes and interfaces. When a method is requested via an object getter, the resolution is as following:

    1. Check the current class for that method name.
    2. Check the leftmost superclass for that method name.
    3. Recursively check that superclass' lefmost superclass, all the way up.

* All classes except base classes (Object, String, Number, and Exception) silently inherit the Object class unless another superclass is specified. This means that every object, no matter what class it comes from, has access to Object methods.
* You can access the superclass methods directly via the *super* keyword. The name resolution path is the same as described in multiple inheritance section. If multiple superclasses (Object included) override a certain method, and you want to use a method from a specific superclass, you may specify that superclass's name in parentheses:
```ruby
class OtherCar(Car, Range): # This combination makes no sense :D
    def init():
        super.init(10, 20) # Uses the init from Car class
        @start = 5
        @stop = 10
        @step = 2
        super.refill(2) # Refill from superclass
        @refill(3) # OtherCar implementation of refill
   end

   def refill(amount):
        print "Doing nothing"
   end

   def has(value):
    # Here we specify that we wish to use the has() method from the Range class
    return super(Range).has(value)
   end
end
```
* Classes themselves are objects, with "class" set to a void object named "Class".
* From within the class, all instance properties have to be accessed via self or @ (i.e, self.fuel is the instance variable, whereas fuel is a constant in the given scope).
* Instance vars and methods are mutable by default from within the class, and don't require their names to start with $.
* Class instances are immutable - you cannot add, remove or change their properties, except from within the class methods.
* Class methods are different than normal functions because they can be overloaded, i.e you can have two functions with the same name, but different number of parameters. This cannot be done in regular key-value object literals:
```ruby
class Writer: # Implicitly inherits Object
    def write(words): print words
    def write(words, times):
        for i in times.times(): @write(words) # method chaining
    end
end
```
* Constructor methods are named *init*. When an object is constructed via class instantation, the interpeter will look up for an *init* method with the appropriate number of parameters. All objects have a default empty constructor that takes no parameters.
* An empty init with parameters is a special construct that creates instance variables for all the parameters. This makes it very easy to construct value object classes without having to write boilerplate code.
```ruby
class Point:
    def init(x, y): pass

    # This is fully equivalent to:
    # def init(x, y):
    #   @x = x
    #   @y = y
    # end

    # In value classes, you may want to override the equals method
    # to check against fields, i.e to use matches() with == operator:
    def equals(other): return @matches(other)
end
```
* All methods in Šimi classes are at the same time static and non-static (class and instance), it's their content that defines if they can indeed by used as both - when referencing a method on an instance, *self* will point to that instance; conversely, it will point to the Class object when the method is invoked statically.
* Methods and instance variables that start with an *underscore* (_) are considered protected, i.e they can only be accessed from the current class and its subclasses. Trying to access such a field raises an error:
```ruby
class Private:
    def init():
        self._privateField = 3
    end

    def _method():
        self._privateField = 2 # Works
    end
end

private = Private()
print private._privateField # Error
print private._method() # Error
```
* Classes that are defined as **class$ Name** are *open classes*, which means that you can add properties to them. Most base classes are open, allowing you to add methods to all Objects, Strings and Numbers:
```ruby
# Adding a method that doubles a number
Number.double = def (): @_ * 2
a = 3
b = a.double() # b == 6
```
* Classes that are defined as **class_ Name** are *final classes*, which can't be subclasses. This feature is important as it normally all subclasses can alter instance fields from their superclasses, so being able to make classes non-subclassable adds to code safety and stability.
* The Object class has a native *builder()* method, meaning that any class in Šimi automatically implements the builder pattern:
```ruby
Car = Car.builder()\
    .brand("Mazda")\
    .model("CX-7")\
    .year(2009)\
    .build()
```

### Operators

#### Arithmetic

+, -, *, /, //, %

* \+ can be used to add values and concatenate strings.
* Other operators work on Numbers only.
* \- Can be used as an unary operator.
* // is the integer division operator, 3 // 2 == 1, while 3 / 2 == 1.5.

#### Assignment

=, +=, -=, *=, /=, %=

#### Logical

not, and, or

* *not* is unary, *and* and *or* are binary
* *and* and *or* are short-circuit operators (and-then and or-else)

#### Comparison

==, !=, <, <=, >, >=, <>

* On objects, == implicitly calls the *equals()* method. By default, it checks if two object *references* are the same. If you wish to compare you class instances based on values, override this method in your class. If you need to check equivalence based on values, check out the *matches()* method.
* The comparison operator <> implicitly invokes *compareTo()* method, which returns -1 if the left compared value is lesser than the right one, 0 is they're equal and 1 if it's greater. For Numbers and Strings, this operator returns the natural ordering, whereas for Objects it can be used in *sorted()* method, as well as a replacement for < and >:
```ruby
obj1 <> obj2 < 0 # Is equivalent to obj1 < obj2
```
 * Remaining operators (<, <=, > and >=) can only be used with Numbers.

#### is and is not
You can check if an Object is instance of a class by using the *is* operator. It can also be used to check types:
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
```

#### in and not in
The *in* operator implicitly calls the *has()* method, that's defined for Objects and Strings, but not for numbers. For strings, it checks presence of a substring. For Objects, it checks presence of a value for arrays, and key for keyed objects. It can be overriden in subclasses, for example in the Range class:
```ruby
class Range:

    # ... rest of implementation omitted ...

    def has(val):
        if @start < @stop: return val >= @start and val < @stop
        else: return val <= @start and val > @stop
    end
end
"str" in "substring" # true
"a" not in "bcdf" # true
2 in [1, 2, 3] # true
"a" in [b = 2, c = 3] # false
range = Range(1, 10)
2 in range # true
10 not in range # true
```

#### ?? - nil coalescence
The ?? operator checks if the value for left is nil. If it is, it returns the right value, otherwise it returns the left value.
```ruby
a = b ?? c # is equivalent to a = ife(b != nil, b, c)
```

#### ? - nil silencing
Using a method call, getter or setter on a nil results in a NilPointerException, but you can silence that by using the ? operator. Basically, the ? operator will check if its operand is nil. If it is, it's going to disregard all the .s and ()s after it, and return a nil value.
```ruby
obj = nil
a = obj.property # CRASH
b = ?obj.property # b = nil, no crash
```

#### @ - self referencing
The @ operator maps exactly to *self.*, i.e @tank is identical to writing self.tank. It's primarily there to save time and effort when implementing classes (when you really write a lot of *self.*s).

### Control flow

#### Truth
When it comes to literal values, determining the truth is easy:
* *nil* is false, and
* 0 (to which the keyword *false* maps to) is false,

while everything else (including empty strings and objects) is true.

#### if-elsif-else
Not much to say here, Šimi offers the usual if-elsif-...-elsif-else structure for code branching.
```ruby
if a < b:
  c = d
  print c
end elsif a < c: print "a < c"
elsif a < d:
  a = e
  print e
end else: print f
```

#### when
A syntax sugar for a lot of elsifs when you're checking against the same value. It supports *is*, *is not*, *in* and *not in* operators; otherwise it assumes that the operator is *==*. You can use the *or* operator to check against multiple conditions in a single branch. *When* statement can be made exhaustive by adding an *else* block at the end.
```ruby
when a:
    5: print "a is 5"
    10 or 13 or 15: print "a is 10 or 13 or 15"
    is String: print "a is String"
    not in Range(12, 16): print "not between 12 and 16"
    else: print "reached the default branch"
end
```

#### The *ife* function
While Šimi does not offer the ternary operator (?:), the Stdlib contains a global function named **ife**, which works exactly as ?: does - if the first parameter is true, the second parameter is returned, otherwise the third parameter is returned. The syntax of the ife function is more readable and forces the user to make use of short, concise expressions for all three parameters.
```ruby
max = ife(a < b, a, b)
```
##### *ife* and lazy loading
If you look at the definition of the *ife* function, you'll see that it has call operator *()* for both of its "branches":
```ruby
def ife(condition, ifval, elseval):
    if condition: return ifval()
    else: return elseval()
end
```
In Šimi, can can call any value other than nil - it's not just limited to functions and classes. If you invoke a call on a Number, String or non-class Object, it will simply return itself. This allows for lazy loading to happen - if a function allows for it, you can pass params inside parameter-less, single-line lambdas (def (): ...), and then those params will be evaluated only when they're used in the said function:
```ruby
step = ife(min < max, 1, -1) # Works with non-function values

# The following example uses functions to lazily compute only the value
# which needs to be returned. Notice ":" syntax, which is shorthand for
# def ():
step = ife(min < max, :Math.pow(2, 32), :Math.pow(3, 10))
```

#### while loop
The *while* block is executed as long as its condition is true:
```ruby
while $a < 10:
  print $a
  $a *= 2
end
```

#### for-in loop
Šimi offers a *for-in* loop for looping over iterables (and virtually everything in Šimi is iterable). The first parameter is the value alias, while the second one is an expression that has to evaluate either to an *iterator* or an *iterable*. The block of the loop will be executed as long as the iterator supplies non-nil values (see section below).
```ruby
for i in 6.times(): print i # Prints 0 1 2 3 4 5 6
for c in "abcdef": print c # Prints a b c d e f
for value in [10, 20, 30, 40]: print value # Prints 10 20 30 40
object = [a = 1, b = "str", c = Pen(color = "blue")]
for key in object: print key # Prints a, b, c
for item in object.enumerate(): print item.key + " = " + item.value # Prints a = 1, b = str, etc.
```
If the expression can't evaluate to an iterator or an iterable, an exception will be thrown.

#### Iterators and iterables
The contract for these two interfaces is very simple:
* An *iterator* is an object that has the method *next()*, which returns either a value or nil. If the iterator returns nil, it is considered to have reached its end and that it doesn't other elements.
* An *iterable* is an object that has the method *iterate()*, which returns an iterator. By convention, every invocation of that method should produce a new iterator, pointing at the start of the iterable object.

Virtually everything in Šimi is iterable:
1. The Number class has methods times(), to() and downto(), which return Ranges (which are iterable).
2. The String class exposes a native iterator which goes over the characters of the string.
3. The Object class exposes a native iterator that works returns values for arrays, and keys for objects. There's also a native enumerate() method, that returns an array of \[key = ..., value = ...] objects for each key and value in the object.
4. Classes may choose to implement their own iterators, as can be seen in the Stdlib Range class.

#### break and continue
The *break* and *continue* keywords work as in other languages, and must be placed inside loops, otherwise the interpreter will throw an exception.

#### return and yield

Control flow in functions is done via *return* and *yield* statements.

The *return* statement behaves as it does in other languages - the control immediately returns to whence the function was invoked, and can either pass or not pass a value:
 ```ruby
 def f(x):
    if x < 5: return 0
    return x
 end
 print f(2) # 0
 print f(6) # 6
 ```

 The *yield* statement is very similar to return, but the function block stores where it left off, and subsequent invocations resume the flow from that point:
```ruby
def testYieldFun():
    print "before yield"
    for i in 3.times():
        if i < 2: yield "yield " + i
        else: return "return in yield"
    end
end
print "Calling yield test"
print testYieldFun()
print "Calling again..."
print testYieldFun()
print "Calling yet again..."
print testYieldFun()
print "Now it should restart"
print testYieldFun()
```
The output of this code is:
```
Calling yield test
before yield # Function is invoked the first time, and it starts at beginning
yield 0 # The value of i is 0, and the function remembers that it "returned" from here
Calling again... # The second invocation will resume where the function left off...
yield 1 # ...and we pick up at the next iteration of the loop, with i == 1
Calling yet again... # Resuming after another yield
return in yield # This time i == 2, so a return is called
Now it should restart # That's exactly right
before yield # ...and we see that it does restart
yield 0
```
Yielding allows for coding of generators, and serves as a lightweight basis for concurrency, allowing a code to do multiple "returns" without having to resort to using callbacks.

### Exception handling

#### Exceptions
All exceptions thrown in Šimi do (and should) extend the base class *Exception*. The default constructor takes a string message, and the class exposes a native method *raise()* that is used for throwing an error.

#### The *rescue* block
Šimi compresses the usual try-catch-...-catch-else-finally exception handling structure into a single concept, that of a *rescue block*.

```ruby
def abs(value):
  if value is not Number: InvalidParameterException("Expected a number!").raise()
  return Math.abs(value)
end

def testAbs():
  value = "string"
  print "This is printed"
  absValue = abs(value) # The exception is thrown here!
  print "This will be skipped"
  rescue ex:
    if ex: print "Exception occurred: " + ex.message
    else: print "An exception did not happen"
    print "This is always executed"
  end
  print "Resuming the block normally..."
end
```
The rescue block works as following:
* If an exception is raised anywhere in the code, the interpreter will start skipping statements until it hits a rescue block, where it will enter, passing the raised exception as the parameter of the rescue block. Program execution will then resume normally at the end of the rescue block.
* If no rescue blocks are found by the time interpreter executes the next global statement, the program will crash.
* If a rescue block is encountered during normal program execution, it is executed with a *nil* parameter.

You can think of all the code above a rescue block as a part of try block, with the catch/except and else blocks being the if/else of the rescue block. The finally part is unecessary as all the statements above the rescue block are a part of the same scope as the statements below it. Check out the example above, rewritten in Python:

```python
def testAbs():
  value = "string"
  print "This is printed"
  try:
    absValue = abs(value) # The exception is thrown here!
    print "This will be skipped"
  except InvalidParameterException:
    print "Exception occurred: " + ex.message
  else:
    print "An exception did not happen"
  finally:
    print "This is always executed"
    print "Resuming the block normally..."
```
As you can see, the *rescue* statement is more concise and leads to less nesting, while allowing to you explore all the possible execution paths.

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
Veggies = Enum.of(["POTATO", "CUCUMBER"], [isRound = def (): return self == Veggies.POTATO])
potato = Veggies.POTATO
print "Is potato round: " + potato.isRound() # true
print "Is cucumber round: " + Veggies.CUCUMBER.isRound() # false
```
Overall, that should satisfy most, if not all the needs that a developer has when leveraging enums in their code. If you go as far as creating enums that have both value objects and functions associated with them, the legibility of *Enum.of()* usage starts to deteriorate and would better be served with a dedicated language construct. Also make sure to check out its implementation in Stdlib to learn more on metaprogramming in Šimi!

### Importing code

Šimi's *import* keyword is used to import code from external files into the current file. Importing is resolved in the pre-processing phase, in which all the imports are recursively resolved until all are taken care of. Each file will be imported only once.

You may import two types of files into your Šimi code:
1. Other Šimi files (must end in ".simi"). Simply put, the entire content of the other file will be loaded in front of the content of the caller file.
2. Native Java code from JARs (must end in ".jar"). The native module manager will scan the JAR for the JavaApi class and try to instantiate it to be used during runtime. Check out the **Java API** section for more info on using native code.

```ruby
# The import keyword is followed by a string denoting the absolute
# or relative path to the imported file.
import "./code.simi" # Imports Simi code
import "./../../NativeCode.jar" # Imports a Java API JAR
import '/Users/gordan/Desktop/awesomeCode.simi'
```

Two files are automatically imported into you file by the interpreter: *Stdlib.simi* and its native companion, *Stdlib-java.jar*. Again, these needn't be imported manually, but make sure that your Stdlib folder (which contains these files) is in your interpreter's root.

### Java API

If you require a functionality that is not readily available in Šimi, and coding it manually might pose a challenge due to complexity or performance issues, you may use the Šimi Java API and expose "native" Java code to the Šimi runtime. Let's examine how this works by checking out the Stdlib *Date* class and its *now()* and *format()* methods.

Here's the Šimi implementation:
```ruby
class Date:
    # Instances will have a single field, named timestamp
    def init(timestamp): pass

    # These methods is implemented natively via Java API. Note the
    # native keyword as the only statement in function body.
    def now(): native
    def format(pattern): native
end
```

A Java project contains the class that'll represent the native equivalent of our Date class:
```java
// The @SimiJavaClass annotation tells the annotation processor that this class represents
// a Simi class. The name parameter may be omitted if the name of the Java class is the
// same as the one of Simi class.
@SimiJavaClass(name = "Date")
public class SimiDate {

    // Expose the Java methods via the @SimiJavaMethod annotation. The name should be
    // the same as the name of the Simi method, and the returned value a SimiValue.
    // All SimiJavaMethods must have at least two parameters, a SimiObject, and a
    // BlockInterpreter. After that, you may supply any number of SimiValue params,
    // which must correspond to the params in the Simi method declaration.

    // The now() method is meant to be used statically, hence the self param will
    // in fact be a SimiClass (Date), which can then be used to create a new
    // Date instance:
    @SimiJavaMethod
    public static SimiValue now(SimiObject self, BlockInterpreter interpreter) {
        SimiClass clazz = (SimiClass) self;
        SimiValue timestamp = new SimiValue.Number(System.currentTimeMillis());
        return clazz.init(interpreter, Collections.singletonList(timestamp));
    }

    // The format method is meant to be used non-statically, meaning that its
    // self parameter will reflect the object on which the method was invoked.
    // This allows us to get its timestamp field and format based on that.
    @SimiJavaMethod
    public static SimiValue format(SimiObject self, BlockInterpreter interpreter, SimiValue pattern) {
        // The self param represents the object which invokes the method. We then use
        // the get(String, SimiEnvironment) method to get the object's "timestamp" field,
        // which we know to be a number.
        long timestamp = self.get("timestamp", interpreter.getEnvironment()).getNumber().longValue();

        // We then use SimpleDateFormat class to format the java.util.Date represented
        // by the timestamp to the specified format string.
        // The returned value must then be wrapped in a SimiValue again.
        return new SimiValue.String(new SimpleDateFormat(pattern.getString()).format(new java.util.Date(timestamp)));
    }
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

You may also expose *global* methods, i.e methods that aren't linked to a class via the *@SimiJavaGlobal* annotation. These methods needn't have the first two parameters set to SimiObject and SimiEnvironment. Global methods should be used to represent a stateless operation, e.g exposing a math function.

(For Cocoa interop, please check out [the iOS/OSX project readme](https://github.com/globulus/simi-ios).)

### Annotations

Šimi supports annotations to allow for association of metadata with classes, functions and other fields. This metadata can then be used at runtime by other parts of the code. Annotations start with **!** and come in form of object literals and precede a declaration:
```ruby
class Api:
    ![method = "GET", endpoint = "/user"]
    def getUser(result):

    end

    ![method = "GET", endpoint = "/appointments"]
    def getAppointments(result): pass
end
```
You may have as many annotations before a field as you'd like, each in a separate line.

You can get a list of field's annotations using the **!!** operator. It returns a list of objects, or *nil* if no annotations have been associated with a field. Note that annotation objects are evaluated upon the !! invocation, meaning that annotations can contain variables and other expressions. Let's examine a simple class that generates SQL code for creating tables based on a list of model classes.
```ruby
# Model superclass. It doesn't have its own table, but contains a primary key field
# that will be used by all model superclasses.
class Model:
    # The dbField in the annotation specifies that this field will have a column in
    # the corresponding table. If its value is true, we'll infer the column name
    # from the field name. Otherwise, we'll use the supplied string value.
    ![dbField = true, primaryKey = true]
    id = 0
end

# We use the dbTable annotation to indicate that this class should have a table in
# the database. The associated value is the name of the table.
![dbTable = "Users"]
class User(Model):
    ![dbField = "first_name"]
    firstName = ""

    ![dbField = "last_name"]
    lastName = ""
end

![dbTable = "Appointments"]
class Appointment(Model):
    ![dbField = true]
    userId = ""

    ![dbField = "timeslot"]
    time = Date()
end

# This class generates SQL code for creating tables based on a list of classes.
class DbLib:
    def init(tables):
        sqlBuilder = String.builder()
        for table in tables:
            @_sqlForTable(table, sqlBuilder)
        end
        @sql = sqlBuilder.build()
    end

    def _sqlForTable(table, sqlBuilder):
        classAnnotations = !!table # First we get annotations for the class
        if not classAnnotations: return # If there aren't any, skip this class
        for annot in classAnnotations:
            dbTable = annot.dbTable # We're interested in the annotation that has "dbTable" field
            if dbTable:
                name = ife(dbTable is String, dbTable, table.name) # Infer table name
                sqlBuilder.add("CREATE TABLE ").add(name).add(" (\n") # Construct SQL
                @_sqlForFields(table, sqlBuilder) # Add column SQL
                sqlBuilder.add(");\n")
            end
        end
    end

    def _sqlForFields(table, sqlBuilder):
        # Iterate through all the class fields and find those that have an annotation
        # which contains the "dbField" field
        for key in table:
            val = table.(key)
            keyAnnotations = !!val
            if not keyAnnotations: continue
            for annot in keyAnnotations:
                dbField = annot.dbField
                if not dbField: continue
                name = ife(dbField is String, dbField, key) # Infer the name
                $type = nil # Infer type based on calue associated with the field in class definition
                if val is Number: $type = "int"
                elsif val is String: $type = "varchar(255)"
                elsif val is Date: $type = "date"
                # ... of course, many more types could be added, including relationships to other tables

                sqlBuilder.add(name).add(" ").add($type)
                if annot.primaryKey: # Check for "primary key" field in the annotation
                    if $type == "int": sqlBuilder.add(" NOT NULL AUTO_INCREMENT,")
                    sqlBuilder.add("\nPRIMARY KEY (").add(name).add("),")
                end
                else: sqlBuilder.add(",")
                sqlBuilder.add("\n")
            end
        end
    end
end

(def ():
    db = DbLib([Model, User, Appointment])
    print db.sql
end)()
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

An obvious power of this combo is that any internal state of a Šimi interpeter can be dumped into code and then interpreted again, making serialization and deserialization of Šimi classes, objects, or any other piece of code trivial. This is similar to what JSON does for JavaScript, with the added benefit of being able to serialize *anything* - functions, classes, annotations, etc:
```ruby
class Car:

    wheels = 4

    def init(capacity, tank): pass

    def refill(amount):
        if amount < 0: CarException("Amount < 0!").raise()
        if amount > 100: TankOverflowException("Too much gasoline!").raise()
        if @tank + amount > @capacity: @tank = @capacity
        else: @tank = @tank + amount
    end

    def refill(amount, doprint):
        @tank = 0
     end
end

carInstance = Car(40, 10)

print ivic Car # printing the class
print ivic carInstance
```
The code above produces the following output:
```
class Car:
    wheels = 4
    def init(capacity, tank):
        self.capacity = capacity
        self.tank = tank
    end
    def refill(amount):
        if amount < 0:
            CarException("Amount < 0!").raise()
        end
        if amount > 100:
            TankOverflowException("Too much gasoline!").raise()
        end
        if self.tank + amount > self.capacity:
            self.tank = self.capacity
        end
        else:
            self.tank = self.tank + amount
        end
    end
    def refill(amount, doprint):
        self.tank = 0
    end
end
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
    c = def (a, b):
        result = a + b + "dddd"
        return result
    end
]
clone = gu ivic obj
print clone.matches(obj) # true
```

These operators also allow for some interesting approaches to metaprogramming. Take, for example, how *gu* is used in *Enum.of()* function to generate the enum class and associate values with it:
```ruby
guStr = "class " + className + "(Enum):
    def init(" + $args + "): pass
    def equals(other): return @matches(other)
end"
clazz = gu guStr

...

for key in obj:
    $val = nil
    if isArray: $val = clazz(key)
    elsif isFirstValueScalar: $val = clazz(obj.(key))
    else:
        args = String.from(obj.(key).values(), ", ")
        constructor =  "clazz(" + args + ")"
        $val = gu constructor
    end
    clazz.(key) = $val
end
```
Combining this with *ivic* allows for creation of programs that *change their code on the fly*. Simply, dump a block/function/method to code with *ivic*, use string manipulation to alter its body, and use it again by supplying the altered code to *gu*. Check out this [simple genetic algorithm](https://github.com/globulus/simi/blob/develop/genetic.simi) to see the duo in action!

### Android integration

You can use Šimi in your Android app. Check out [our other project](https://github.com/globulus/simi-android)!

### iOS integration

You can use Šimi in your iOS app. Check out [our other project](https://github.com/globulus/simi-ios)!

### To-Dos

Here's a list of features that might make it into the language at some point in the future:
1. **async/function yield** expression: a function would yield until the execution of another function is done, and then resume its execution. This would allow for clearer code, without using callbacks. The basis for this is already implemented via the *yield statement*.
```ruby
def post(body):
    request = Request(body, [cookie = "123])

    # The execution of post() will stop here, and resume once executePost() returns.
    result = yield NetworkingLib.executePost(request)

    if result ...etc etc...
end
```
**This is currently supported via the [Stdlib Async class](stdlib/Async.simi).**

2. **Decorator annotations**: if an annotation supplied to a function is a function, the annotation function would be executed as a wrapper for the annotated function whenever the latter is invoked. This would allow for some and concise code, e.g when coding a networking API, in which you'd decorate your functions with networking library wrappers, which would then be invoked whenever your functions are. I unsure about this one as it would make the annotation part of function definition, and the resulting confusion might prove to be a large drawback.
**This is currently supported via the [Decorator class](stdlib/Decorator.simi).**
