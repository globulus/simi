### Annotations

Annotations allow you to associate *metadata* with classes and their members. This metadata can then be accessed at runtime.

An annotation is an object or a call proceeded by exclamation mark *!*:
```ruby
![type = "annotation1", value = 23]
class MyClass {
    ![data = [1, 2, 3]]
    someField = "default value"

    !AnnotationClass(1, "b", 3)
    fn method(a, b, c) {

    }
}
```

Each class or its member can have any number of annotations, listed below each other:
```ruby
![value = "this is 1st annotation"]
!ThisIsSecondAnnotation()
!THIRD_ONE_CONSTANTS_ARE_ALSO_ALLOWED
class MyClass
```

Annotations are evaluated at runtime, when the interpreter encounters them.

#### Accessing annotations at runtime

You can access annotations of a class and its members at runtime using the postfix operator *!*. It only works on classes - invoking it on anything else is a runtime error. If neither the class nor any of its members have any annotations, the operator returns an empty object ([]). Otherwise, it returns an object where keys are names and values are lists of annotations for that object (or objects themselves if it's an inner class).

