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

# A struct (which can also be accomplised via Class instancing)
point = [x = 10, y = 20]

# An array/list is created as a regular object, but without any keys
mutableList = $[1, 2, 3, 4, 5]
mutableList.push(6)
immutableList = ["str", "str2", "str4"]

# Sets are arrays which invoked the native set() function
set = mutableList.set()

# Tuples are basically immutable lists/arrays
tuple = [10, 20, "str"]

# Dictionary/Map/Hash
mutableObject = $[key1 = "value1", key2 = 12, key3 = [1, 2, 3, 4]]
immutableObject = [key4 = mutableObject.key3.len(), key5 = 12.345]

# Immutable objects with functions can be used as static classes
basicArithmetic = [
    add = def (a, b): return a + b
    subtract = def (a, b): return a - b
]
```

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

#### break and continue
The *break* and *continue* keywords work as in other languages, and must be placed inside loops, otherwise the interpreter will throw an exception.

### Exception handling - the *rescue* block
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
