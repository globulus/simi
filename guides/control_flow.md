### Control flow

#### Truth
In Šimi, **only the boolean value of *false* is not true**. Everything else (including empty strings and objects, numberical value of zero, and nil) is true.

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

The parentheses around conditions are optional, and so are the braces around the post-condition statement, if there's only one of it (multiple statements require a block, of course).

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

The *when* equivalent different only as it allows the use of *=* for the single-line expression block:
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
fn extractValue(obj) = when obj {
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

#### while loop
The *while* block is executed as long as its condition is true:
```ruby
while a < 10 {
  print a
  a *= 2
}
```

#### do-while loop
The *do-while* loop also executes while its condition is true, but its condition is checked at the end, meaning its body is always going to execute at least once.
```ruby
a = 10
do {
  print a # This will still get printed because condition is evaluated at the end
  a *= 2
} while a < 3
```

#### for-in-if nil loop
Šimi offers a *for-in* loop for looping over iterables (and virtually everything in Šimi is iterable). The first parameter is the value alias, while the second one is an expression to iterate over. The block of the loop will be executed as long as the iterator supplies non-nil values (see section below).
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
for [key, value] in object.zip() {
 print key + " = " + value # Prints a = 1, b = str, etc.
}
```

If the expression is *nil*, the loop won't run. However, if it isn't nil and doesn't resolve to an iterator, a runtime error will be thrown.

There are situations in which you want to know that a loop didn't run because its iterator was nil. In such cases, append an *if nil* block after the for-loop:
```ruby
for i in something {
    # do your thing
} if nil {
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
The *break* and *continue* keywords work as in other languages - break terminates the active loop, while continue jumps to its top. They must be placed in loops, otherwise you'll get a compile error.

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

If you follow the *do* block with an *else* block, the *else* block will be executed *if the do block breaks*. If the *do* block reaches its end normally, the *else* block is discarded.
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

*do-else* blocks can be used as *procs* - local functions that *aren't closures*, i.e they full share the same scope within which they were declared. You can think of a proc as a zero-argument subroutine - calling it jumps to its start, it does its thing, and when it reaches its end it jumps back to the call site. The important trait of a proc is that, since it is not a closure, it can alter its surrounding scope.

Define a proc by assigning a *do-else* block to a variable. The block(s) must be expression blocks, i.e their last value is the block's return value, just as it did with [if and when expressions](#if-and-when-expressions).

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
i = p() # Call the proc, i will be 20
j = p() # Call the proc, j will be 5
```

Just like a do-block, a proc can return a value early with *break*:
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
i = p() # Call the proc, i will be 20
j = p() # Call the proc, j will be 10
```

A proc can also have an *else* part, in case of which it behaves like a *do-else* block:
1. If the do-block reaches its end without breaking, it return its last line.
2. If the do-block breaks, its break value binds to *it* in the else block, and the last line of the else block is the return value.
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
i = p() + 5 # i will be 30
j = p() # j will be 25
```

#### return and yield
Control flow in functions happens via *return* and *yield* statements.

The *return* statement behaves as it does in other languages - the control immediately returns to whence the function was called, and can either pass or not pass a value:
 ```ruby
 fn f(x) {
    if x < 5 {
        return 0
    }
    return x
 }
 print f(2) # 0
 print f(6) # 6
 ```

 A *return* always returns from the current function, be it a lambda or not. Some languages make a distinction there, but Šimi does not.

 The *yield* statement is very similar to *return*, but it's only applicable to [fibers](coroutines.md). Please check the Fibers section for more info.
