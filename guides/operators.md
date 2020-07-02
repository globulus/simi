### Operators

#### Addition

\+

* When both operands are Nums, it performs addition.
* When either operand is a String, the non-String operand is *stringified* and the two strings are concatenated.
* When the left-hand operand is a mutable List, the right-hand operand is appended to that List.

#### Arithmetic

-, *, /, //, %

* Only work on numbers.
* \- Can be used as an unary operator.
* // is the integer division operator, 3 // 2 == 1, while 3 / 2 == 1.5.

#### Assignment and compound assignment

=, $=, +=, -=, *=, /=, //=, %=, ??=

#### Logical

not, and, or

* *not* is unary, *and* and *or* are binary.
* *and* and *or* are short-circuit operators (and-then and or-else).

#### Equality

==, !=

The equality operator checks if two values are, well, equal. The thing is, it's overloadable to a degree:
* If the provided values are in fact the same, i.e the same number, same string or the reference to the same function/object/class/whatever, it returns true.
* If this isn't the case and provided values are objects, it looks up if the said object (or one of its superclasses) overloads the *equals()* method. If so, it invokes the method and returns its result. This allows for implementation of custom definitions of equality based on object types - e.g, two Ranges are equal if their start and stop values are the same:
```ruby
class Range {
    # ... rest of implementation omitted ...

    fn equals(other) = start == other.start and stop == other.stop
}
```

Again, you can't override the default behaviour and say that two references to the same thing aren't equal, because that would: a. be silly, b. it would force you to handle that use-case manually, which means boilerplate code.

The operator that checks for inequality is *!=*. Since *THIS != THAT* is just *not (THIS == THAT)*, overloading *equals* affects *!=* as well. 

> *Design note:* I'm aware that using "!=" for "not equal" seems out of place, especially since "not" is a separate keyword and "!" is used in other capacities elsewhere in the language, but it just seems natural coming from C. I haven't come up with a better solution so far.

#### Comparison

<, <=, >, >=

The usual stuff - less than, less or equal, greater than, greater or equal. All of those operate on numbers only.

#### Range operators

*Range* is an oft-used Core class, so it has its own dedicated set of operators that operate on numbers.

* *..* (two dots) creates a range from left hand side *until* right hand side - up to, but not including:
```ruby
1..10 # 1, 2, 3, 4, 5, 6, 7, 8, 9
```
* *...* (three dots) creates a range from left hand side *to* right hand side - up to, and including:
```ruby
1...10 # 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
```

Internally, these operators are mere syntax sugar that compiles as invocations of Num.rangeTo and Num.rangeUntil methods, respectively. (Not that it matters as Num is a final class so these methods can't be overridden.)

#### is and is not
You can check if a value is of a certain type with the *is* operator. Its inverted version is *is not* (and not *not is*, primarily because it reads more naturally).

The right hand side must be a class. Since everything in Šimi is an object (or can be boxed into one), you can dynamically check the type of anything on the left hand side:
* *nil is Anything* always returns false.
* For numbers: var is Num
* For Strings: var is String
* For functions: var is Function
* For fibers: var is Fiber
* For classes: var is Class
* For objects: var is Object
* For lists: var is List
* To check is an object is an instance of a class or any of its subclasses: var is SomeClass.
* You can also use *is* to see if a class is a subclass of another class: SomeClass is PotentialSuperClass.
    * > *Design note:* This part is debatable, as *is* is supposed to check *what type a value is*, but then again, it flows well with the *is* keyword for subclassing. It might be removed in the future, or replaced with *>* (although that one isn't without issues either).

```ruby
a = [1, 2, 3, 4]
a is Object # true
a is not Number # true
b = 5
b is Number # true
b is String # false
car = Car("Audi", "A6", 2016)
car is Car # true
car is not Object # false

TODO expand
```

#### in and not in
The *in* operator implicitly calls the *has()* method. This means that *in* is a fully overloadable operator, just be mindful it's operands are inverted: THIS in THAT is equal to THAT.has(THIS). It's negated variant is *not in*. Since *THIS not in THAT* is just *not (THIS in THAT)*, overloading *has* affects *not in* as well. 
 
 Method *has()* is defined for Objects, Lists and Strings, but not for Numbers.
 * For Objects, it checks if the provided value is the object key.
 * For Lists, it checks if the value is in the list.
 * For strings, it checks if the provided value, which must be a String, is a substring.

Again, this method can be overriden in any class. See, for example, how it's done in the Range class:
```ruby
class Range {
    # ... rest of implementation omitted ...

    fn has(val) = if start < stop {
            val >= start and val < stop
        } else {
            val <= start and val > stop
        }
    }
}
"str" in "substring" # true
"a" not in "bcdf" # true
2 in [1, 2, 3] # true
"a" in [b = 2, c = 3] # false
range = Range(1, 10)
2 in range # true
10 not in range # true
```

#### ?? - nil coalescence
The *??* operator checks if the value for left is nil. If it is, it returns the right value, otherwise it returns the left value. This operator is short-circuit (right won't be evaluated if left is not nil).
```ruby
a = b ?? c # is equivalent to a = if b != nil b else c, just faster
```
You can use *??=* with variables to assign a value to it only if its current value is nil:
```ruby
a = nil
a ??= 5 # a is 5
b = 3
b ??= 5 # b is 3
```

#### @ - self referencing
*@* isn't a real operator - it maps exactly to *self.*, i.e *@tank* is identical to writing *self.tank*. It's primarily there to save time and effort when implementing classes (when you really write a lot of *self.* s).

### ! - get annotations
The postfix *!* operator retrieves the list of annotations associated with a field. For more details on usage, check out [Šimi annotations](#annotations).

#### Bitwise operations
All bitwise operations (and, or, xor, unary complement, shift left, shift right, unsigned shift right) are implemented as methods on the Number class:
```ruby
print 12.bitAnd(25) # prints 8
```

#### Precedence and associativity

| Precedence | Operators                   | Description                                        | Associativity |
|------------|-----------------------------|----------------------------------------------------|---------------|
| 1          | . ?. () ?() !               | (safe) get, (safe) call, get annotations           | left          |
| 2          | not - gu                    | invert, negate, gu                                 | right         |
| 3          | ??                          | nil coalescence                                    | left          |
| 4          | / // * %                    | division, integer division, modulo, multiplication | left          |
| 5          | + -                         | addition, subtraction                              | left          |
| 6          | .. ...                      | range until, range to                              | left          |
| 7          | < <= > >= <>                | comparison, matching                               | left          |
| 8          | == != is is not in not in   | (in)equality, type tests, inclusion test           | left          |
| 9          | and                         | logical and                                        | left          |
| 10         | or                          | logical or                                         | left          |
| 11         | = $= += -= *= /= //= %= ??= | assignment                                         | right         |
| 12         | ?!                          | rescue                                             | left          |