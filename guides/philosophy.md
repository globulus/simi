### Šimi - the basic philosophy

Let's outline what Šimi is about with two basic tenets of the language:
1. Things done often should be simple to do. The language should have baked-in features for tasks that developers perform multiple times on a daily basis. An example of this is the plus operator (+) used for concatenating lists.
2. The language should be kept simple and with as few gotchas as possible. The syntax should be consistent.

Guides to learn how to use the language. These aren't necessarily meant to be read in a specific order - skim through them to get a basic sense of the language, and then go back to them later on at your own discretion, to learn more on a particular topic in greater detail.
* Learn about the [basic syntax](syntax.md) and learn about some basic safety tenets of Šimi - scoping, lack of local variables and prohibited name shadowing.
* Read on [primitive values](basic_values.md).
* The [operators](operators.md) guide lists all the Šimi operators, their precedence and lists examples of their usage.
* [Control flow](control_flow.md) guide gives detailed overview of all the Šimi control flow constructs - branchings, expression branchings, loops, breakable blocks and procs.
* [Functions](functions.md) are first-class citizens in Šimi. Learn all about defining them, arguments (name and default), lambdas, shorthand syntax, etc.
* [Objects](objects_lists.md) are the central compound data structure in Šimi. The second most often used structure, [lists](objects_lists.md).
* Šimi is a full OOP language and the [classes](classes.md) guide gives an in-depth overview of all the tools at your disposal - defining classes, types of classes, their methods and fields, initializers, inheritance, mix-ins and extensions.
* [Enums](enums.md) are special classes that solve a common problem.
* Fibers are Šimi [coroutines](coroutines.md). Learn how to define them, instantiate and then call, and also how to switch between different fiber runtimes.
* Šimi code should be [modular](modularity.md). Learn how to organize your code, link it with other code.
* Importing native functionality in Šimi is easy with the [native API](native_api.md). Currently, this is only JVM.
* No language is complete without [exception handling](exception_handling.md). Šimi has a somewhat unique take on this problem.
* Šimi classes and their properties can be [annotated](annotations.md) to associate meta-data with them in order to generate code by itself.
* While being a dynamic language, Šimi offers the [safety package](safety_package.md) to make runtime type checking a thing.
* No language is complete without a [debugger](debugging.md). Learn how to use it to walk though your programs step-by-step and inspect their execution.
* 