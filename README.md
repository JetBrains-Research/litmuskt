# LitmusKt 

**LitmusKt** is a litmus testing tool for Kotlin.
Litmus tests are small concurrent programs exposing various relaxed behaviors, arising due to compiler or hardware
optimizations (for example, instruction reordering).

This project is in an **experimental** stage of the development. 
The tool's API is unstable and might be a subject to a further change.

## Setup

Simply clone the project and run `./gradlew build`.

Note that for Kotlin/JVM this project relies on [jcstress](https://github.com/openjdk/jcstress), so you also need to
set `JCS_DIR` environment variable to `/path/to/jcstress/folder`.

Tip: if you wish to run Gradle tasks from IDEA, you need to set `JCS_DIR` for it too, and a handy way to do that is to
run IDEA from bash: `idea & disown -a` (note the singular `&`).

## Running

The entry point is the CLI tool residing in `:cli` subproject. You can use the `--help` flag to find the details about
the CLI, but most basic example requires two settings:

1. Choose a runner with `-r` option
1. After the options are specified, choose the tests to run using regex patterns

### Running on Native

Create an executable and run it:

```bash
./gradlew :cli:linkReleaseExecutableLinuxX64
./build/bin/linuxX64/releaseExecutable/cli.kexe --help
```

Depending on what you need, you can:

* Switch between `debug` and `release` (which, among other things, toggles the `-opt` compiler flag)
* Specify the platform (`linuxX64` / `macosX64` / `macosArm64`)

### Running on JVM

The entry point is still the `:cli` subproject. Simply run the project with Gradle:

```bash
./gradlew :cli:jvmRun --args="--help"
```

## Overview

A single litmus test consists of the following parts:
* a state shared between threads;
* code for each thread;
* an outcome &mdash; a certain value which is the result of running the test;
* a specification listing accepted and forbidden outcomes

The tool runs litmus tests with various parameters, 
using the standard techniques also employed by other tools, 
like [herdtools/litmus7](https://github.com/herd/herdtools7) and [JCStress](https://github.com/openjdk/jcstress).

The tool allocates a batch of shared state instances
and runs the threads on one state instance after another, 
occasionally synchronizing threads with barriers. 
After all threads finish running, states are converted into outcomes, and the same outcomes are counted. 
The end result is the list of all different observed outcomes, 
their frequencies and their types (accepted, interesting or forbidden).

### Litmus Test Syntax

Here is an example of the `LitmusKt` test:

```kotlin
class StoreBufferingState(
  var x: Int = 0,
  var y: Int = 0,
  var r1: Int = 0,
  var r2: Int = 0,
)

val StoreBuffering = litmusTest(::StoreBufferingState) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    outcome {
        listOf(r1, r2)
    }
    spec {
        accept(listOf(0, 1))
        accept(listOf(1, 0))
        accept(listOf(1, 1))
        interesting(listOf(0, 0))
    }
}
```

And here is an example of the tool's output:

```
 outcome |    type     |  count  | frequency 
---------------------------------------------
 [1, 0]  |  ACCEPTED   | 6298680 |  48.451%  
 [0, 1]  |  ACCEPTED   | 6291034 |  48.392%  
 [0, 0]  | INTERESTING | 405062  |  3.1158%  
 [1, 1]  |  ACCEPTED   |  5224   |  0.0401%  
```

Let us describe the litmus test's declaration.

* As a first argument `litmusTest` takes a function producing the shared state instance.
* The second argument is DSL builder lambda, setting up the litmus test.  
* `thread { ... }` lambdas set up the code run in different threads of the litmus tests — 
   these lambdas take shared state instance as a receiver.
* `outcome { ... }` lambda sets up the outcome of a test obtained after all threads have run —
  these lambdas also take shared state instance as a receiver.
* the `spec { ... }` lambda classifies the outcomes into acceptable, interesting, and forbidden categories. 

Here are a few additional convenient features.

* Classes implementing `LitmusAutoOutcome` interface set up an outcome automatically.
  There are a few predefined subclasses of this interface. 
  For example, the class `LitmusIIOutcome` with `II` standing for "int, int" expects two integers as an outcome. 
  This class have two fields `var r1: Int` and `var r2: Int`. 
  These fields should be set inside litmus test's threads, and then they will be automatically used to form an outcome `listOf(r1, r2)`.
 
* If the outcome is a `List`, you can use a shorter syntax for declaring accepted / interesting / forbidden outcomes. 
  Just use `accept(vararg outcome)` counterparts to specify expected elements.
 
* Since each test usually has its own specific state, it is quite useful to use anonymous classes for them.

Using these features, the test from above can be shortened as follows:

```kotlin
val StoreBuffering: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
        interesting(0, 0)
    }
}
```

### Litmus Test Runners

Litmus tests are run with a `LitmusRunner`. This interface has several running functions:

* `runTest(params, test)` simply runs the test with the given parameters.
* `runTest(duration, params, test)` repeatedly runs the test with the given parameters until the given time duration passes.
* `runTestParallel(instances, ...)` runs several instances of the test in parallel.
* `runTestParallel(...)` without explicit instances number will run `#{of cpu cores} / #{of threads in test}` instances.

The following implementations of `LitmusRunner` are available:

* For native:
  * `WorkerRunner`: based on K/N `Worker` API
  * `PthreadRunner`: based on C interop pthread API
* For JVM:
  * `JvmThreadRunner`: a simple runner based on Java threads
  * `JCStressRunner`: a **special** runner that delegates to JCStress. Note that many of `LitmusRunner` parameters are
    not applicable to JCStress.

### Litmus Test Parameters

There is a number of parameters that can be varied between test runs. Their influence on the results can change
drastically depending on the particular test, hardware, and so on.

* `AffinityMap`: bindings from thread to CPU cores. 
  Obtained through `AffinityManager`, which is available from `getAffinityManager()` top-level function.
* `syncEvery`: the number of tests between barrier synchronizations.
  Practice shows that on Native the reasonable range is somewhere in the range from 10 to 100,
  while on JVM it works best in the range from 1000 to 10000.
  This highly depends on the particular test.
* `Barrier`: can be either Kotlin-implemented (`KNativeSpinBarrier`) or C-implemented (`CinteropSpinBarrier`). 
  C-implemented might yield better results.
  On JVM, use `JvmSpinBarrier` in favor of `JvmCyclicBarrier`.

Common practice is to iterate through different parameter bundles and aggregate the results across them.
* Function `variateParameters()` takes the cross-product of all passed parameters 
  (hence use `listOf(null)` instead of `emptyList()` for unused arguments).
* For results aggregation, use `List<LitmusResult>.mergeResults()`.
* You can also use `LitmusResult.prettyPrint()` to print the results.

### Project structure

The project consists of several subprojects:

* `:core` contains the core infrastructure such as `LitmusTest` and `LitmusRunner` interfaces, etc.
* `:testsuite` contains the litmus tests themselves.
* `:codegen` uses KSP to collect all tests from `:testsuite`.
* `:jcstress-wrapper` contains the code to convert `LitmusTest`-s into JCStress-compatible Java wrappers.
* `:cli` is a user-friendly entry point.

## Notes

* If you decide to add some litmus tests, you **must** put them into `:testsuite` subproject and
  into `komem.litmus.tests` package. Deeper packages are allowed.
* Setting thread affinity is not supported on macOS yet. As such, `getAffinityManager()` returns `null` on macOS.
* It is possible to run the tests with `@Test` annotation. However, the tests are run in debug mode by
  the `kotlinx.test` framework. Running litmus tests in the debug mode can affect their results, potentially hiding some
  relaxed behaviors.
* In practice, all cases of currently found relaxed behaviors can be consistently found in under a minute of running.
* Avoid creating unnecessary objects inside threads, especially if they get shared. This not only significantly slows down the performance, but can also introduce unexpected relaxed behaviors.
* The tool currently doesn't address the false sharing problem. The memory shuffling API is in development.
