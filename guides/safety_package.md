### The safety package

Šimi is a primarily a dynamically typed language, meaning that its syntax and behavior are tailed towards maximal flexibility at runtime. Examples of this is the ability to assign any value to any variable, pass any value as a parameter, freely call any value or perform gets and invoked on a *nil*.

However, nobody is denying the advantages of strong typing, especially for larger and more serious codebases. Because of this, Šimi includes a set of features dubbed "the safety package", which allow for runtime type and null-checks to make sure that the code does exactly what you want it to under all circumstainces.

#### Argument and return type checks
You can add runtime type checks to function args and return types:
```ruby
fn func(a is String, b is Num = 20, c is Range = Range(3, 40)) is Range? {
    ...
}
```

1. You specify the type by putting *is* behind the argument name, followed by the type.
    a. Types are non-nullable by default, i.e "a is String" will fail if *nil* is provided for *a*. To specify that the type is nullable, add *?* at the end of the type: "a is String?".
2. The function return type is checked by putting the *is TYPE* check after the arguments.
    a. The nullability rule applies here as well.
    b. If your function TODO LINK exceptions returns exceptions instead of nil, you can mark the return type as being either it or exception with *!*: "fn func() is String!" means that this function should return either a String or an Exception.

Again, these checks are performed at runtime, and will return *TypeMismatchException* if something is off.

#### Nil-safe gets and calls
Šimi permits operations on *nil*, such as getters, setters and calls. All of those return nil and don't produce an exception:
```ruby
a = nil
a.something # nil
a() # nil
```

While being a normal feature of a nullable dynamically-typed language, such code can produce errors that are difficult to trace and fix. To combat this, Šimi has nil-safe gets and calls: *?.* and *?(*. You might've seen this in some statically-typed languages, where they're used on nullable values and return null if the value really is null. In Šimi, it's *the other way around* - normal calls return nil if the value is nil, while safe calls return a *NilReferenceException*:
```ruby
a = nil
a?.something # Safe get, the result is a NilReferenceException instance
a?() # Safe call, the result is a NilReferenceException instance
# You can chain safe and unsafe calls and gets in any order you want
obj.nullableProp?(1, 2, 3).funcThatMayReturnNil("a")?.nullableFunc?()
```

The returned NilReferenceException can be handled with a [rescue block](exception_handling.md), just like any other exception:
```ruby
value = a?.something().that?() ?! {
    return 2
}
```