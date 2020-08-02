### Exception handling

#### Exceptions
All exceptions in Šimi do (and should) extend the base class *Exception*. The default constructor allows you to define a string message for your exception. It's generally advisable to create your own exception subclass as opposed to discerning exceptions based on their message.

#### Returning exceptions
In Šimi, Exceptions **are returned, and not thrown/raised**. If a certain piece of code wishes to indicate that an exception has occurred, it should return an instance of the Exception class or any of its subclasses. It's a simple as that, and also dead-easy to understand how does control flow in the function where the exception occurred - it immediately returns to its call site.

Just as you probably guessed, this *does not* unwind the call stack in search of the nearest catch block - the catch block needs to be right there at the call site.

#### Catch blocks
A catch block sits at the end of a statement and waits for an exception to be returned from any of the calls or getters. If this happens, it immediately executes the code within its block. After that (or otherwise, if the statement doesn't return any exceptions) life goes on as it did before.

A catch blocks starts with *catch*, followed by a block. This is a hard block, and not an expression one, and you can thing of it as sitting *below* the statement it catches, and not inline with it.

If the catches block triggers when a call returns an exception, it will be bound inside the catch block as *it*, so that you may access it.

Let's look at a few examples:
```ruby
# Here's a function that can return an exception
fn validateAmount(a) {
    return if a >= LIMIT {
        ValidationException("\(a) is over the limit of \(LIMIT)")
    } else {
        a
    }
}

validateAmount(someAmount) catch {
    print "Validation failed: \(it.message)"
}

result = validateAmount(20) catch {
    print "Not valid"
    result = 35 # The catch block is executed "after" the expression before it
}

doSomething().thenSomethingElse().thenAnotherThing() catch {
    print it
}
```

If you need a common handler for a lot of catch blocks, pair it with a do-else, *break it* in all the catch blocks and see the magic unfold:
```ruby
do {
    someCallThatMightProduceExceptionA() catch { break it }
    a = thisMight.returnExceptionB + thisMightReturnSomeOtherException() catch { break it }
} else {
    print when it {
        is ExceptionA = "exception A occurred \(it.message)"
        is ExceptionB = "exception A occurred \(it.message)"
        else = "something else \(it)"
    }
}
```

The *it* in the catch block (the exception) is transferred via *break* to the *else* block, where it's also bound to the local *it*.

> *Design note:* It is important for exception handling to be as local as possible, for readability.