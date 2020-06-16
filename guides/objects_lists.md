#### List and object comprehensions
Comprehensions make generating lists and object based on other data quicker to write, read and execute. Imagine you're tasked with generating a list of squares of even numbers up to 10. One route you might take is to use a for loop:
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

The comprehensions have a few advatanges, though:
1. You can specify if the created list/object is mutable or not.
2. Objects can filter and map keys and values at the same time.
3. They're easier to read and type.
4. They're much faster to execute due to the way they're compiled.