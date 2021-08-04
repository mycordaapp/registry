# The 'Registry' Dependency Injection (DI) Pattern

[![Travis Build Status](https://travis-ci.com/mycordaapp/registry.svg?branch=master)](https://travis-ci.com/mycordaapp/registry) [travis]
[![Circle CI](https://circleci.com/gh/mycordaapp/registry.svg?style=svg)](https://circleci.com/gh/mycordaapp/registry) [circleci]
[![Licence Status](https://img.shields.io/github/license/mycordaapp/registry)](https://github.com/mycordaapp/registry/blob/master/licence.txt)

## Overview

An incredibly simple DI pattern, that essential stores all dependencies in a HashMap and supports lookup either by class
or interface name.

Deployed to [jitpack](https://jitpack.io/com/github/mycordaapp/registry/0.0.3). To include in your project, if using
gradle:

```groovy 

\\ add jitpack repo 
maven { url "https://jitpack.io" }

\\ include the dependency 
implementation 'com.github.mycordaapp:registry:0.0.3'
```

## How it works

The `Registry` simplifies a "framework less" pattern for DI whereby the dependencies are wired by passing them as
construct parameters. The `Registry` allows a single parameter to be passed into the constructor. The idea is borrowed
from the [Ratpack](https://ratpack.io/) MVC framework.

As a simple example, the service Foo takes the interfaces Red and Green, and the class BlueThing as dependencies:

```kotlin
interface Red
interface Green
interface Blue

class RedThing : Red {}
open class GreenThing : Green {}
class BlueThing : Blue {}

// service with constructor params 
class FooService(private val red: Red, private val green: Green, private val blueThing: BlueThing) {}

val foo = FooService(RedThing(), GreenThing(), BlueThing())
```

With the `Registry`, we store instances in the `Registry` and then extract as required. This extraction can be at any
time, but typically it makes sense to do so in the constructor:

```kotlin
// service with registry 
class FooService(registry: Registry) {
    private val red = registry.get(Red::class.java)
    private val green = registry.get(Green::class.java)
    private val blueThing = registry.get(BlueThing::class.java)
}

val reg = Registry().store(RedThing()).store(GreenThing()).store(BlueThing())
val foo2 = FooService(reg)
```

It is an implementation decision whether to include a regular constructor as well.

There are three simple rules to remember:

* The `Registry` mutates. If you want to keep a "safe" copy long term, it is best to call `clone()`. Of course if the
  dependencies are extracted immediately in the constructor this is rarely a problem.
* The lookup is either by interface or class, but in both cases there must only be a single instance that matches. This
  obviously makes use of generic interfaces and classes problematic. In some cases it might be necessary to construct a
  simple wrapper to avoid ambiguity.
* The `Registry` is not thread safe. Application initialisation code that stores in the `Registry` should be single
  threaded.

As an example, we now include shape interfaces, and store some implementing classes in the `Registry`.

```kotlin
interface Square
interface Circle
interface Triangle

class GreenSquare : GreenThing(), Square {}
class GreenCircle : GreenThing(), Circle {}
class ASquare : Square {}
class ATriangle : Triangle {}
class MySpecialGreenThing : GreenThing() {}

reg.store(GreenSquare())
    .store(GreenCircle())
    .store(ASquare())

// fine, only one Circle
val circle = reg.get(Circle::class.java)

// fine, only one BlueThing
val blueThing = reg.get(BlueThing::class.java)

// fails - what type of Square. GreenSquare or ASquare?
val square = reg.get(Square::class.java)

// fails - there are 2 classes with GreenThing in their hierarchy
val greenThing = reg.get(GreenThing::class.java)

// fails - no Triangle stored in the repo 
val triangle = reg.get(Triangle::class.java)

// fine - we use the default ATriangle
val triangle = reg.getOrElse(Triangle::class.java, ATriangle())

// fine - even though there are multiple GreenThing classes, there is a default provided
val greenThing = reg.getOrElse(GreenThing::class.java, MySpecialGreenThing())
```

Common interfaces and classes don't work well as they are too likely to be ambiguous. In practice this is rarely a
problem for well-designed class hierarchies with clear and unambiguous names. But, if necessary, just create a simple
wrapping class:

```kotlin
// Don't store "String" - it's not clear what the String refers to and 
// is potentially ambiguous  
reg.store("db=foo;user=root;password=secret")

// Instead create a simple wrapping class
data class DatabaseConnectionString(val connection: String)
reg.store(DatabaseConnectionString("db=foo;user=root;password=secret"))
```

In some cases there maybe just a class name (one example may be when passing information over an API - there is use of
this pattern in the [Tasks](https://github.com/mycordaapp/tasks) project). To support this there are also variants that
work through a fully qualified class name. See below:

```kotlin
// assume all the setup code above 

// finding a Circle by class name. Note an explicit 
// cast may be necessary 
val circle = reg.get("com.example.Circle") as Circle

// fails. Must pass a fully qualified name
val circle = reg.get("Circle") as Circle

// For completeness, all the helper methods are available when looking up 
// by class name, though in most cases they are probably of limited use,
val triangle = reg.getOrElse("com.example.Triangle", ATriangle())

```

In this usecase, there are two advantages in using the registry over custom code:

* the necessary reflections code is already written
* the registry is a natural "sandbox", and will guard against malicious attacks to instantiate unexpected classes

## Benefits of this approach

* **minimal dependencies** - `Registry` is under two hundred lines of code.
* **very lightweight** - no hidden startup time while classes are scanned for annotations and a dependency tree is built
  in memory
* **encourages good design** - some will argue this point, but my personal experience has been that explicitly wiring up
  DI in the source code leads to better designs - the "auto magic", convention driven frameworks like Spring can lead to
  hidden complexities.
* **minimally opinionated** - the only opinion is that classes should support at least one clearly defined constructor
  to control DI, which is very arguably just good design anyway.

## Differences with this approach

* **explicit `Registry`** - the most obvious difference with say Spring is that whereas as in Spring there is
  an `ApplicationContext` from which we retrieve wired objects (that is, class instances that have all their
  dependencies resolved and injected) in this model we explicitly wire-up the dependencies via the constructor.
* **explicit `request` scoping** - in the richer frameworks there are lifecycle events to manage dependencies that are
  tied to other scopes, such as `RequestScoped` which is linked to an `HttpServletRequest`. In the `Registry` model they
  must be wired up manually with the selected MVC framework, though this is rarely more than a few lines of code.
* **no startup validation** - unlike say Spring or Google Guice, there is no explicit validation at startup. Any
  problems will only surface once an instance is requested. I will argue that is actually a benefit - the registry
  pattern is so lightweight it can be used in any unit test and so any problems are likely caught early. 
  
