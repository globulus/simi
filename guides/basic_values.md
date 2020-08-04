### Basic values

This guide provides an overview of basic, non-compound Šimi types: booleans, numbers and strings. The other types: TODO links objects, lists, classes, functions, fibers and modules are described in more detail in separate sections.

#### Nil

*nil* is a singleton that represents an absence of value. In a way, *nil* always points to itself - any operation on it also results in nil.

#### Booleans
Boolean values can take only two values: *true* or *false*. The most important usage of the boolean type is that, in Šimi, **false is the only *false* value - everything else is true**. TODO LINk Logical, equality and comparison operators all return booleans. TODO LINk Conditional statements all require booleans to operate.

Do note that Šimi doesn't have implicit type conversion (LINK beyond that when concatenating strings), therefore booleans can't interoperate with numbers, like they can in some languages.
```ruby
a = true
b = false
c = a or b # c is true
if a { ... }
else if b { ... }
else {
    d = a + 20 # This is a runtime error, luckily this branch won't be reached
}
```

#### Numbers
At surface, Šimi doesn't make a distinction between integers and floating-point numbers, but instead has one number type, 8 bytes long.

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

String interpolation is supported by enclosing the nested expressions into *$(SOMETHING)*. Single, non-keyword identifiers don't need the parentheses, they can just be preceeded by a *$*:

```ruby
a = 2
b = 3
print "a is $a, and doubled it is $(a * 2) and b, which is $b, minus 1 halved is $((b - 1) / 2)"
# Prints: a is 2, doubled is 4 and b, which is 3, minus 1 halved is 1
```

When used as objects, strings are boxed into an instance of open Core class *String*, which contains useful methods for string manipulation. Since strings are immutable, all of these methods return a new string.

Strings are pass-by-value, and boxed strings are pass-by-reference.

The String class contains a native *builder()* method that allows you to concatenate a large number of string components without sacrificing the performance that results from copying a lot of strings:
```ruby
class Range {

    ... rest of implementation omitted ...

    fn toString = String.builder()\
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
fn times = ife(@_ < 0, =Range(@_, 0), =Range(0, @_)).iterate()
```