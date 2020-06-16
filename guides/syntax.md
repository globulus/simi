### Basic syntax

For the most part, Šimi is a C-like language, so most devs should feel at home right from the start.

#### Keywords
Šimi has 26 reserved keywords:
```ruby
and break class continue def do else false fib for gu if import in 
is ivic native nil not or print return self super true when while yield
```

#### Comments
```ruby
# This is a single line comment
print b # They span until the end of the line

#* And here is
a multiline comment
spanning multple lines *#
```

> *Design note* - why use # for comment start instead of //? 1. It's one character as opposed to two. 2. I wanted to leave // for integer division.

#### Identifiers
Identifiers start with a letter, or _, and then may contain any combination of letters, numbers, or underscores. Identifiers are case-sensitive.
```ruby
# Here are some valid identifiers
a
a123
_abc
___abc123_345a__b54___
```

#### Newlines
Line breaks separate Šimi statements and as such are meaningful. E.g, an assignment statement must be terminated with a newline. The compiler will usually warn you if a newline is missing.

If your line is very long, you can break it into multiple lines by ending each line with a backslash *\\* for readability purposes.
```ruby
a = 5 # This is a line
# Here are two lines, the first one nicely formatted by using \
arr = [1, 2, 3]\
    .reversed()\
    .joined(with = [5, 6, 7])\
    .where(def i = i <= 5)\
    .map(def i = i * 2)\
    .sorted(def (l, r) = r.compareTo(l))
```

On the other hand, if you wish to pack multiple short statements onto a single line, you can separate them with a semicolon *;*. To the lexer, a newline and a semicolon are the same token.
```ruby
print arr; print a # And here are two print statements separated by ;
```

#### Code blocks
A block of code starts with a left brace *{*, followed by one or multiple statements, terminated with a right brace *}*. This allows the blocks to span an arbitrary number of lines and always have a clearly designated start and end.
```ruby
if a < 5 { print a } # Single line block
else { # Multiline block
    a += 3
    print b
}
```

#### Variables
Declare a variable with the *=* operator:
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

TODO LINK Compound operators (+=, -=, etc.) are invoked with $= by default. Also, $= cannot be used with set expressions as they're mutating by default.

##### Constants
Some variables are real constants and using $= with them will result in a compiler error. These are:
1. Variables whose name is in CAPS_SNAKE_CASE.
2. Declared classes, functions and fibers, regardless of their case.
3. Variables declared with *_=* instead of *=*. The *_=* operator was introduced primarily to allow the SINGLETON OBJECTS LINK to be declared as constants.
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

#### Scoping
Šimi uses lexical scoping, i.e a variable is valid in the block it was declared in and its nested blocks:
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

**Name shadowing is prohibited and results in a compile error.** Vast majority of the time, there's no good reason to shadow a variable from a parent scope, and it makes the code more difficult to read and more prone to errors.
```ruby
a = 5
{
    a = 6 # Compile error, a was already declared in an outer block
}
```