# Šimi
Šimi (*she-me*) is small, object-oriented programming language that aims to combine the best features of Python, Ruby, JavaScript and Swift into a concise, expressive and highly regular syntax.

### Basic syntax

#### Keywords
Šimi has 25 reserved keywords:
```ruby
and break class continue def else elsif end false for if import in is
native nil or pass print rescue return self super true while
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
# Pass is widely used in native functions, as their bodies need to be empty
native len(): pass
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

Šimi supports exactly 5 values, making it extremely easy to infer types and know how does a part of code behave:???
1. Number - equivalent to a *double* in other languages, represents 64-bit floating-point number. Also serves as the boolean type, with *true* mapping to 1, and *false* mapping to 0.
2. String - represents a (multiline) piece of text, enclosed in either single or double quotes.
3. Function - a parametrized block of code that can be invoked. Functions are fist-class citizens in Šimi and can be passed around as normal values.
4. Object - the most powerful Šimi construct, represents a mutable or immutable array/list, set, dictionary/map/hash, or a class instance.
5. Nil - represents an absence of value. Invoking operations on nils generally results in a NilPointerException.

#### Numbers
Šimi doesn't make a distinction between integers and floating point numbers, but instead has only a double-precision floating point type, which doubles as a boolean type as 0 represents a false value.

When numbers are used as objects, they're boxed into an instance of open Stdlib class $Number, which contains useful methods for converting numbers to strings, rounding, etc.

Numbers are pass-by-value, and boxed numbers are pass-by-copy.

#### Strings
Strings are arrays of characters. They can be enclosed either in single or double quotes. You can freely put any characters inside a string, including newlines. Regular escape characters can be included.
```ruby
string = "this is a
    multiline
 string with tabs"
anotherString = 'this is another "string"'
```
When used as objects, strings are boxed into an instance of open Stdlib class $String, which contains useful methods for string manipulation. Since strings are immutable, all of these methods return a new string.

Strings are pass-by-value, and boxed strings are pass-by-copy.

The $String class contains a native *builder()* method that allows you to concatenate a large number of string components without sacrificing the performance that results from copying a lot of strings:
```ruby
class Range:

    ... rest of implementation omitted ...

    def toString():
        return $String.builder()\
            .plus("Range from ").plus(@start)\
            .plus(" to ").plus(@stop)\
            .plus(" by ").plus(@step)\
            .build()
    end
end
```

##### Boxed numbers and strings
Boxed numbers and strings are objects with two fields, a class being $Number of $String, respectively, and a private field "_" that represents the raw value. The raw value can only be accessed by methods and functions that extend the Stdlib classes, and is read-only. Using the raw value alongside the @ operator results in the so-called *snail* lexeme:
```ruby
# Implementation of times() method from $Number class
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

# Functions that map to native code have empty bodies
native pow(a, b): pass

# Functions without parameters are required to have parentheses
def printFunction(): print "printing"

# A lambda function passed to filter an array, has implicit return if it's single line
# Lambdas with a single parameter needn't put the parameter in parentheses
filteredArray = [1, 2, 3, 4, 5].filter(def i: i > 2)
```
Functions start with the *def* keyword if they're implemented in Šimi, or with *native* keyword if they're implemented externally. Native functions are required to have empty bodies (i.e, a single *pass* statement).

Functions in class bodies are called *methods* and have a few caveats to them, which we'll cover in the Classes section.

Functions can be invoked by using (), and passing arguments in them. You can *name* any parameter in the function by putting a name and an equals sign before the value. This helps readability, and the Šimi interpreter will disregard anything up to and including the equals sign for a given parameter.
```ruby
sum = add(3, 4)
difference = subtract(a = 5, b = pow(2, 3))
printFunction()
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

The underlying class for objects is the open Stdlib class $Object. It contains a large number of (mostly native) methods that allow the objects that facilitate their use in any of the purposes in the list above.

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

* All classes except base classes ($Object, $String, $Number, and Exception) silently inherit the $Object class unless another superclass is specified. This means that every object, no matter what class it comes from, has access to $Object methods.
* You can access the superclass methods directly via the *super* keyword. The name resolution path is the same as described in multiple inheritance section. If multiple superclasses ($Object included) override a certain method, and you want to use a method from a specific superclass, you may specify that superclass's name in parentheses:
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
class Writer: # Implicitly inherits $Object
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
end
```
* All methods in Šimi classes are at the same time static and non-static (class and instance), it's their content that defines if they can indeed by used as both - methods that have references to *self* in their bodies are instance-only as the *self* will be nil when used on a class.
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
* Classes whose names start with $ are *open classes*, which means that you can add properties to them. Most base classes are open, allowing you to add methods to all Objects, Strings and Numbers:
```ruby
# Adding a method that doubles a number
$Number.double = def (): @_ * 2
a = 3
b = a.double() # b == 6
```
* The $Object class has a native *builder()* method, meaning that any class in Šimi automatically implements the builder pattern:
```ruby
Car = Car.builder()\
    .brand("Mazda")\
    .model("CX-7")\
    .year(2009)\
    .build()
```

### Operators

#### Arithmetic

+, -, *, /, %

* \+ can be used to add values and concatenate strings.
* Other operators work on Numbers only.
* \- Can be used as an unary operator.

#### Assignment

=, +=, -=, *=, /=, %=

#### Logical

not, and, or

* *not* is unary, *and* and *or* are binary
* *and* and *or* are short-circuit operators (and-then and or-else)

#### Comparison

==, !=, <, <=, >, >=, <>

* On objects, == implicitly calls the *equals()* method. By default, it checks if two object *references* are the same. If you wish to compare you class instances based on values, override this method in your class.
* The comparison operator <> implicitly invokes *compareTo()* method, which returns -1 if the left compared value is lesser than the right one, 0 is they're equal and 1 if it's greater. For Numbers and Strings, this operator returns the natural ordering, whereas for Objects it can be used in *sorted()* method, as well as a replacement for < and >:
```ruby
obj1 <> obj2 < 0 # Is equivalent to obj1 < obj2
```
 * Remaining operators (<, <=, > and >=) can only be used with Numbers.

#### is and is not
You can check if an Object is instance of a class by using the *is* operator. It can also be used to check types:
```ruby
a = [1, 2, 3, 4]
a is $Object # true
a is not $Number # true
b = 5
b is $Number # true
b is $String # false
car = Car("Audi", "A6", 2016)
car is Car # true
car is not $Object # false
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

#### The *ife* function
While Šimi does not offer the ternary operator (?:), the Stdlib contains a global function named **ife**, which works exactly as ?: does - if the first parameter is true, the second parameter is returned, otherwise the third parameter is returned. The syntax of the ife function is more readable and forces the user to make use of short, concise expressions for all three parameters.
```ruby
max = ife(a < b, a, b)
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
1. The $Number class has methods times(), to() and downto(), which return Ranges (which are iterable).
2. The $String class exposes a native iterator which goes over the characters of the string.
3. The $Object class exposes a native iterator that works returns values for arrays, and keys for objects. There's also a native enumerate() method, that returns an array of \[key = ..., value = ...] objects for each key and value in the object.
4. Classes may choose to implement their own iterators, as can be seen in the Stdlib Range class.

#### break and continue
The *break* and *continue* keywords work as in other languages, and must be placed inside loops, otherwise the interpreter will throw an exception.

### Exception handling

#### Exceptions
All exceptions thrown in Šimi do (and should) extend the base class *Exception*. The default constructor takes a string message, and the class exposes a native method *raise()* that is used for throwing an error.

#### The *rescue* block
Šimi compresses the usual try-catch-...-catch-else-finally exception handling structure into a single concept, that of a *rescue block*.

```ruby
def abs(value):
  if value is not $Number: InvalidParameterException("Expected a number!").raise()
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

### Java API

If you require a functionality that is not readily available in Šimi, and coding it manually might pose a challenge due to complexity or performance issues, you may use the Šimi Java API and expose "native" Java code to the Šimi runtime. Let's examine how this works by checking out the Stdlib *Date* class and its *format()* method.

Here's the Šimi implementation:
```ruby
class Date:
    # Instances will have a single field, named timestamp
    def init(timestamp): pass

    # This method is implemented natively via Java API. Note the native
    # keyword and the empty body.
    native format(pattern): pass
end
```

A Java project contains the class that'll represent the native equivalent of our Date class:
```java
// The @SimiJavaClass annotation tells the annotation processor that this class represents
// a Simi class. The name parameter may be ommitted if the name of the Java class is the
// same as the one of Simi class.
@SimiJavaClass(name = "Date")
public class SimiDate {

    // Expose the Java methods via the @SimiJavaMethod annotation. The name should be
    // the same as the name of the Simi method, and the returned value a SimiValue.
    // All SimiJavaMethods must have at least two parameters, a SimiObject, and a
    // SimiEnvironment. After that, you may supply any number of SimiValue params,
    // which must correspond to the params in the Simi method declaration
    // (native format(pattern): pass).
    @SimiJavaMethod
    public static SimiValue format(SimiObject self, SimiEnvironment environment, SimiValue pattern) {
        // The self param represents the object which invokes the method. We then use
        // the get(String, SimiEnvironment) method to get the object's "timestamp" field,
        // which we know to be a number.
        long timestamp = self.get("timestamp", environment).getNumber().longValue();

        // We then use SimpleDateFormat class to format the java.util.Date represented
        // by the timestamp to the specified format string.
        // The returned value must then be wrapped in a SimiValue again.
        return new SimiValue.String(new SimpleDateFormat(pattern.getString()).format(new java.util.Date(timestamp)));
    }
}
```
After that, everything's really simple: build the Java project and import its resulting JAR into Šimi code:
```ruby
import "SimiDate.jar"

date = Date(1516004607682)
print date.format("dd/MM/yyyy hh:mm:ss")
```
You'll notice that there's no performance degradation associated with using native methods as the code is accessed via reflection only once, when the JAR import is resolved. After that, all native method references are effectively statically typed.

You may also expose *global* methods, i.e methods that aren't linked to a class via the *@SimiJavaGlobal* annotation. These methods needn't have the first two parameters set to SimiObject and SimiEnvironment. Global methods should be used to represent a stateless operation, e.g exposing a math function.
