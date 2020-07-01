### Exception handling

#### Exceptions
All exceptions in Šimi do (and should) extend the base class *Exception*. The default constructor allows you to define a string message for your exception. Its generally advisable to create your own exception subclass as opposed to discerning exceptions based on their message.

#### Returning exceptions

In Šimi, Exceptions **are returned, and not thrown/raised**. If a certain piece of code wishes to indicate that an exception has occurred, it should return an instance of the Exception class or any of its subclasses. It's a simple as that, and also dead-easy to understand how does control flow in the function where the exception occurred - it immediately returns to its call site.

Just as you probably guessed, this *does not* unwind the call stack in search of the nearest catch block, as there's no such thing in Šimi. Instead, exceptions are handled via **rescue blocks**.

#### Rescue blocks

A rescue block sits at the end of a statement and waits for an exception to be returned from any of the calls or getters. If this happens, it immediately executes the code within its block. After that (or otherwise, if the statement doesn't return any exceptions) life goes on as it did before.

A rescue blocks starts with *?!* (you can read this as "if exception"), followed by a block. This is a hard block, and not an expression one, and you can thing of it as sitting *below* the statement it rescues, and not inline with it.

If the rescue block triggers when a call returns an exception, it will be bound inside the rescue block as *it*, so that you may access it.

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