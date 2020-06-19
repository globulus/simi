### Enums

An enum is a class that has a finite set of constant values, listed in enum declaration. Instantiating an enum is a runtime error. The value declaration must be the first declaration in an enum, before the fields, init, methods, etc.
```ruby
enum Color {
    RED, BLUE, GREEN
}
color = Color.RED
color = Color() # Nope, runtime error
```

Just like other classes, enums can have methods and store data:
```ruby
enum State {
    NEW, IN_PROGRESS, FINISHED, ERROR
    
    def isDone = when self {
        State.FINISHED or State.ERROR = true
        else = false
    }
}
```

An enum can have *init* to bind data to its instances. If so, the enum values should use that *init*:
```ruby
enum Mountain {
    MT_EVEREST(8848), K2(8611), KANGCHENJUNGA(8586), LHTOSE(8516)
    
    init(@height)
}
```