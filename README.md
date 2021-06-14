# Registry Dependency Injection (DI) Pattern 

## How it works

The registry simplifies a "framework" less pattern for DI whereby the dependencies 
are wired by passing them as construct parameters. The registry allows a single 
parameter to be passed.

As a simple example, the service Foo takes the interfaces Red and Green, and the class 
BlueThing as dependencies: 

```kotlin
interface Red
interface Green
interface Blue

class RedThing : Red {}
open class GreenThing : Green {}
class BlueThing : Blue {}

// service with constructor params 
class FooService (private val red: Red, private val green : Green, private val blueThing: BlueThing) {}

val foo = FooService(RedThing(),GreenThing(),BlueThing())
```

With the registry, we store instances in the Registry and then extract as required. This 
extraction can be at any time, but typically it makes sense to do so in the constructor:

```kotlin
// service with registry 
class FooService2(registry: Registry) {
    private val red = registry.get(Red::class.java)
    private val green = registry.get(Green::class.java)
    private val blueThing = registry.get(BlueThing::class.java)
}

val reg = Registry().store(RedThing()).store(GreenThing()).store(BlueThing())
val foo2 = FooService2(reg)
```

It is an implementation decision whether to include a regular constructor as well. 

There are two basic rules to remember:
* The registry mutates. If you want to keep a "safe" copy long term, it is best to 
  call clone(). Of course if the dependencies are extracted in the constructor this is 
  never a problem.   
* The lookup is either by interface or class, but in both cases there must only be a 
  single object that matches. This obviously makes use of generic interfaces and 
  classes problematic. In some cases it might be necessary to construct a simple 
  wrapper to avoid ambiguity.
  
As an example, we now include interfaces Square and Circle and store some implementing
classes in the registry.

```kotlin
interface Square
interface Circle

class GreenSquare : GreenThing(), Square {}
class GreenCircle : GreenThing(), Circle {}
class ASquare : Square {}

reg.store(GreenSquare()).store(GreenCircle()).store(ASquare())

// fine, only on Circle
val circle = reg.get(Circle::class.java)

// fine, only one BlueThing
val blueThing = reg.get(BlueThing::class.java)

// fails -what type of Square?
val square = reg.get(Square::class.java)

// fails -what type of GreenThing?  
val greenThing = reg.get(GreenThing::class.java)
```

In practice this is rarely a problem for well-designed class hierarchies with clear
and unambiguous names. But, if necessary,
just create a simple wrapping class:

```kotlin
// Don't store "String" - it's not clear what the String refers to and potentially 
// ambiguous  
reg.store("db=foo;user=root;password=secret")

// Instead create a simple wrapping class
data class DatabaseConnectionString(val connection : String)
reg.store(DatabaseConnectionString("db=foo;user=root;password=secret"))
```

## Benefits of this approach 

* **minimal dependencies** - `Registry` is just one hundred or so lines of code
* **very lightweight** - no hidden startup time while classes are scanned for annotations \
* **encourages good design** - some will argue this point, but my personal experience has been 
  that explicitly wiring up DI in the source code leads to better designs - the "auto magic", 
  convention driven frameworks like Spring can lead to hidden complexities.
* **minimally opinionated** - the only opinion is that classes should support 
  at least one clearly defined constructor to control DI, which is very arguably just good
  design anyway.
  
## Differences with this approach 

* **explicit `registry`** - the most obvious difference with say Spring is that 
  whereas as in Spring there is an `ApplicationContext` from which we retrieve 
  wired objects (that is, class instances that have all their dependencies resolved and
  injected) in this model we explicitly wire-up the dependencies via the constructor. 
* **explicit `request` scoping** - in the richer frameworks there are lifecycle events to 
  manage dependencies that are tied to other scopes, such as RequestScoped
  HttpServletRequest. In this model they must be wired up manually with the selected MVC 
  framework, though this is rarely more than a few lines of code.
* **no startup validation**. Unlike Spring or Google Guice, there is no explicit validation at 
  startup. Any problems will only surface once an instance is requested. I will argue that 
  is actually a benefit - the registry pattern is so lightweight it can be used in any unit 
  test and so any problems are likely caught early. 
  
