### Functions

Functions are callable blocks of code. Functions can have arguments passed to them, and can return values to their call site, just as in other languages.

#### Function anatomy
Define a function with the **fn** keyword, followed by a name, argument list and the body:
```ruby
fn function(arg1, arg2, arg3) {
    print arg1
    c = arg2 + arg3
    return c
}
```

The arguments are optional, and the argument list can be omitted if it's empty:
```ruby
fn arglessFunc {
    print "doing something"
}
```

The body is optional as well, especially for [abstract methods](classes.md) and [native methods](native_api.md):
```ruby
fn iAintDoingMuchRightNow(a, b, c)
native nativeBodiesAreAlwaysEmpty(a, b)
```

#### Calling functions
The basic function operation is a *call* - invoking the function with its designated arguments so that its body executes and returns a value. A call always involves a pair of parentheses, even if the function doesn't receive any arguments.
```ruby
fn func(a, b) {
    return a + b
}
print func(2, 3) # prints 5
```

Šimi function are first-class citizens of the language, meaning they can be stored in variables and passed around as arguments, just like any other values:
```ruby
fn function(a, b, c) { ... }
var1 = function
var1(1, 2, 3)

fn secondOrderFn(otherFn) {
    print otherFn(1, 2, 3)
}
secondOrderFn(function)
```

> *Design note:* You can now see why are parentheses important for calls - otherwise, we wouldn't know if we're passing the function value or invoking it. Of course, there are syntax sugar examples in other languages, where, say, a function with two arguments can be invoked as an infix operator, but that complicates the syntax and negatively affects code readability, especially if you lack a powerful IDE.

When calling functions, you can pass names to parameters to improve readability. These take form of *identifier = value*, but don't affect the call in any way, as they're just ignored by the compiler:
```ruby
function(a = 3, b = 5, c = 4) # Just adds some readability
```

#### Returning values
A **return** statement aborts function execution and jumps back to its call site. A return statement may or may not return a value - if no expression is specified after the *return* keyword, the default value is *nil*. Also, all functions have an implicit *return nil* at their bottom:
```ruby
fn implicitlyReturnsNil() {
    print "a"
}

fn explicitReturnWithImplicitNil() {
    for item in collection {
        if item.isBad() {
            return # nil is implicit
        }
    }
}

fn explicitReturn(name) {
    return "My name is: \(name)."
}
```

#### Default argument values
Function arguments can have default values. It also implicitly overloads the function by arity. All the arguments with default values must come after all the arguments without default values:
```ruby
fn fnWithDefaultArgs(a, b, c = 13, d = "string") {
    ....
}

fnWithDefaultArgs(1, 2, 3, "d")
fnWithDefaultArgs(1, 2, 3) # equals fnWithDefaultArgs(1, 2, 3, "string)
fnWithDefaultArgs(1, 2) # equals fnWithDefaultArgs(1, 2, 3, "string")
fnWithDefaultArgs(1) # An error, two args are required
```

> *Design note:* Why default arguments as opposed to overloading functions by arity?

#### Expression functions
Functions whose body boils down to a single return can use the *expression function* syntax sugar - just place *=* instead of the body, and omit the return:
```ruby
fn expressionFn(a, b) = a + b # is the same as { return a + b }
```

Such functions are quite common, both as regular functions, methods or lambdas.

#### Lambdas
Function creation can be an expression. This allows you to create anonymous functions (lambdas). The syntax is the same as with regular function, just omitting the name:
```ruby
secondOrderFunction(fn (a, b, c) { return a + b + c})

fnStoredInVar = fn (a, b) {
    print a + b
}
fnStoredInVar(2, 3)
```

#### Implicit arguments for lambdas
A common use-case for lambdas, especially when passed as arguments to higher-order functions, is to return a value based on a set of well-established arguments. Consider the List *where* method, which filters list values based on a predicate:
```ruby
filteredList = list.where(fn (value) = value > 5)
```

Since everyone knows what does the *where* function expect, you can type this even quicker with *implicit arguments*:
```ruby
filteredList = list.where(=$0 > 5)
```

You can omit the *fn* keyword and the argument list and jump straight to the equals sign that indicates a *return*. Implicit arguments start with a dolar sign and are sequentially numbered starting from 0: *$0, $1, $2*. The compiler looks the lambda body up and discovers these implicit arguments, checks if they're properly ordered and synthesizes them for you. The number of allowed implicit arguments isn't limited.

Implicit arguments aren't just for expression lambdas, regular lambdas can use them as well:
```ruby
print fn {
    return $0 + $1
}(1, 2) # prints 3
```

Just remember that improvements to conciseness shouldn't be made at the expense of readability.

#### Closures
All Šimi functions are closures - they have access to variables declared outside their scope and hold onto them. These variables are immutable from within the function

#### self and self(def)
If the called function isn't bound (i.e, it isn't a method), *self* will be bound to it, the function. For methods, you can access the function via the *self(def)* construct:
```ruby
fn standaloneFn() {
    print self # <fn standaloneFn 0>
    print self(def) # <fn standaloneFn 0>
}

class MyClass {
    fn method() {
        print self # prints the instance as defined by toString
        print self(def) # <fn method 0>
    }
}
```

CHECK OUT THE SAFETY PACKAGE

