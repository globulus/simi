### Code organization and modularity
It's natural for a growing codebase to be dispersed among multiple files. Similarly, if you're building a library for other people to use, it will come in its own file(s).

#### Importing external code
Šimi allows you to import code from external files with the *external import*:
```ruby
import "Date"
import "../otherlib/Lib"
```

Imports are resolved between lexing and compiling phases - each Šimi code file specified will be lexed and added upstream for compiling, with its external imports resolved recursively. Each file will be imported only once.

Files specified for external import are assumed to be Šimi files (.simi), so you needn't specify the extension. If the full path to the file isn't provided, it's assumed to live in the same directory as the file it's being imported to.

After all the files have been lexed and all the imports resolved, the entire imported codebase will be compiled as a single file.

The *Core* file and its native companion are implicitly imported into every Šimi code file, so they don't have to be specified manually.

Native dependencies are imported with *import native* - check out the TODO LINK native API guide for that.

#### Modules
Since the compiler doesn't know which code came from which file, it compiles all of it as a part of a single scope. For larger codebases, especially ones that have a multitude of dependencies, this can lead to name collisions. To solve this, Šimi introduces namespaces via *modules*:
```ruby
module MyModule {
    field = 2 + 2 * 2
    singleton = [name = "This", method = =_0 + _1]

    class Class1
    
    class Class2 {
        ...
    }
    
    fn someFunc() {
        ...
    }
    
    fib SomeFib() {
        ...
    }
    
    module Submodule {
        fn otherFunc = 5
    }
}
```

Define modules with the *module* keyword. Within their bodies, they can contain declarations of classes, functions, fibers and constants - just like classes can. However, since modules aren't instantiated, their functions and fibers aren't bound, i.e they won't have *self* defined. Also, module constants are assigned at compile-time, unlike class fields which are assigned during instantiation (in the *init* method). Modules can contain other nested modules.

Reference module's field just as you do with classes, using the dot operator:
```ruby
someFib = MyModule.SomeFib()
print MyModuke.Submodule.otherFunc()
```

#### On-site imports
You can import fields of a module directly into any scope by an *on-site import*:
```ruby
a = 5
b = 6
import MyModule for Class1, field
import MyModule.Submodule for otherFunc
print a + b + field
```

On-site import binds the module's fields specified after *for* to their namesake local variables in the current scope.

If you use a fully qualified name for a module, you can import all its fields with **for \***:
```ruby
import MyModule for *
import MyModule.Submodule for *
```

The compiler will check if the provided qualified name resolves to a previously compiled module. If it doesn't, you'll get a compile error. If you're not using *for \**, the expression after *import* will be evaluated at compile-time, allowing you to use variables, strings, or whatever else you'd like.

Since on-site imports can be placed virtually anywhere, they allow you to be judicious with where is a certain name applicable. TODO FIX

#### Combo file and on-site imports
When dealing with modules, it's quite common to have a single module in a file of the same name - e.g, the file *io.simi* contains a module named *Io*. It'd be a hassle to have to import first the file, and then the module one after another in another file:
```ruby
import "warp_nacelles/Io"
import Io for *
```

Šimi comes to the rescue again by allowing a combo import, functionally fully equivalent to the example above:
```ruby
import "warp_nacelles/Io" for *
```

The preprocessor will import the code from the file at the provided path, and then the compiler will attempt to compile an on-site import for the module based on the *capitalized last path component* ("warp_nacelles/io" will be come "Io").