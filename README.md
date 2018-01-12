# Šimi
Šimi (*she-me*) is small, object-oriented programming language that aims to combine the best features of Python, Ruby, JavaScript and Swift into a concise, expressive and highly regular syntax.

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
3. The $Object class exposes a native iterator that works returns values for arrays, and keys for objects. There's also a native enumerate() method, that returns an array of [key = ..., value = ...] objects for each key and value in the object.

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
