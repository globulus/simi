### Fibers

Fibers are Šimi **coroutines** - autonomous, suspendable runtimes that can pass data between themselves. You can think of a fiber as a suspendable function, one that can execute itself to a certain point, switch the execution back to its caller, and then *resume where it left off* on the next call. It suspends and resumes can both pass data.

Any Šimi program is always running inside an implicit, main fiber (which you can't yield out of).

Let's illustrate all of this with an example.

The first step towards your first fiber is to write a *fiber class*:
```ruby
fib MyFiber(a, b, c) {
    foreach(a..b, fn (i) { # foreach is a function that iterates through first arg and calls second arg with the iteration value
        yield i + c
    })
}
```

The syntax looks awfully like that of functions, because ultimately all a fiber does, code-wise, is wrap a single function (do note the keyword *fib* as opposed to *fn*). However, in order to use a Fiber, you have to instantiate it:
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