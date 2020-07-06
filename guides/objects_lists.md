### Objects and lists

Objects are a centerpiece of Šimi, its most flexible and powerful construct. Objects are omnipresent - even TODO LINK primitive values get wrapped in objects, lists are objects, and so are all classes and their instances - then again, the one of Šimi's goals was to make objects simple to understand and work with.

#### Object basics and object literals
An object is a sequence of key-value pairs. Keys are strings or identifiers, while a value can be any Šimi value - number, string, boolean, object, list, etc. Nothing more or less to it - an object contains some data and some functionality, all baked together and accessible via its keys. Each key-value pair constitutes a **field**.

Objects and be **mutable** or **immutable**. Immutable object's key-value set is fixed to the outside user - you can access the fields, but can't add, remove or update them, **unless done through object's bound functions or class methods**. Mutable objects are, of course, free to be altered in any way by anyone.

Declare an object with an *object literal* - a set of "key = value" pairs, separated by commas, enclosed in brackets:
```ruby
myFirstObject = [name = "Mike", age = 30, work = fn { print "Yep, working" }]

populations = $[tokyo = 37_400_068, delhi = 28_514_000, shanghai = 25_582_000] # Notice the $ before the opening [
```

In essence, the Šimi notion of objects is most similar to that of JavaScript, which I like because of its simplicity - people intuitively understand maps/hashes/dictionaries, and presenting objects as such structures demystifies the concept.

#### Accessing object fields
Access object fields with the dot operator **.**, just as in virtually any other language.
```ruby
print myFirstObject.name # Mike
print myFirstObject.age # 30
```

If you're passing something other than an identifier, wrap it in parentheses:
```ruby
fieldToAccess = "name"
print myFirstObject.(fieldToAccess) # Mike
```

This is called a *getter* - an expression that gets a field from an object.

The **.()** construct is akin to the *subscript operator []* in other languages - any expression within the parentheses will be evaluated and the object will return the corresponding value.

Accessing an invalid key returns **nil**:
```ruby
print myFistObject.invalidKey # nil
```

A getter's counterpart is a *setter*:
```ruby
populations.cairo = 20_076_000 # Added a new field to the mutable object
populations.tokyo = 37_400_069 # Updated an existing field
```

Again, these kinds of setters work on mutable objects. A self-setter works on any object as it's invoked from within.

All objects have a *class*. The basic class (which (almost) all other classes inherit) is "Object". Objects created via object literals have "Object" as their class. You can access an object's class through the special **class** field:
```ruby
print myFirstObject.class # Object
```

Unless your object (or its class) override the *toString* method, stringifying an object will print it in the literal form: it starts with either [ or $\[ (depending on if it's immutable or not), it'll contain a class and a list of its field, closed off by \].

#### self and internal modification
Any object can access itself from within itself with the **self** keyword (most C-like language, the equivalent keyword is *this*). All object functions can access self, and, more importantly, use it to modify object's fields. This is why we say that Šimi objects are always mutable from within. It also allows the developer to use immutable objects more often than mutable ones, since you full retain control over when and how can a field change:
```ruby
obj = [a = 5, fn increment {
    self.a += 1
}]
print obj.a # 5
obj.increment()
print obj.a # 6
```

Typing **self.** over and over again can be tiresome, so Šimi allows you to use **@** instead:
```ruby
obj = [a = 5, fn increment {
    @a += 1
}]
```

Using @ instead of *self.* is idiomatic and is used in all code examples. Usage of *self* is limited to instances where it's not followed by a dot (i.e, it's used as an identifier).


#### List basics and list literals
You're most likely already familiar with lists from other languages - they are ordered and their values are indexed. In Šimi, Lists can contain mixed values of any type.
```ruby
list = [100, "abc", 300, =_0 * _1 - _2]
```

Lists are also objects (their base class, "List", inherits the "Object" class). This means that virtually everything that was said earlier about objects works for lists as well.

Access list elements with an index:
```ruby
print list.0 # 100
print list.2 # 300
```

If you're supplying a raw integer as the index, the parentheses aren't needed. Otherwise, use the parentheses as the subscript operator:
```ruby
list = [1, 2, 3, 4, 5]
i = 2
print list.(i) # 3
```

#### List and object comprehensions
Comprehensions make generating lists and object based on other data quicker to write, read and execute. Imagine you're tasked with generating a list of squares of even numbers up to 10. One route you might take is to use a for-loop:
```ruby
list = List()
for i in 1..10 {
    if i % 2 == 0 {
        list.add(i * i)
    }
}
```

A list comprehension makes this much more concise:
```ruby
list = [for i in 1..10 if i % 2 == 0 do i * i]
```

The *if* part is optional:
```ruby
list = [for item in otherList do item + " something"]

# You can create both mutable and immutable lists with comprehensions,
# just put $ in front:
mutableList = $[for item in otherList if item > 10 do item] 
```

An object comprehension has to provide a key-value pair, separated by *=*:
```ruby
object = [for i in 1..5 do "key\(i)" = i]
# Just like in regular for-loops, object decomposition can be used here as well
object2 = $[for [k, v] in otherObject if k not in ForbiddenKeys and v < 10 do k = v * 10]
```

In most cases when mapping a list or an object from another list or object, the list comprehension is semantically equal to a filtering followed by mapping:
```ruby
list = other.where(=_0 < 10).map(=_0 * 2)
# this is exactly the same as
list = [for i in other if i < 10 do i * 2]
```

The comprehensions have a few advantages, though:
1. You can specify if the created list/object is mutable or not.
2. Objects can filter and map keys and values at the same time.
3. They're easier to read and type.
4. They're much faster to execute due to the way they're compiled.