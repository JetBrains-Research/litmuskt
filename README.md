# Litmus testing on Kotlin/Native

This project is a work-in-progress on a tool for running litmus tests written in Kotlin.

**Note**: everything here is experimental, **arbitrary** changes may occur at any moment.

## Setup

Clone the project.
* On macOS, this is enough.
* On Linux, after cloning go to `src/nativeInterop` directory and run the `setup.sh` script. It compiles some C source files and sets up C interop. The output path must be absolute, so by default, it outputs the object files to `~/.komem-litmus/`. You can change the location to any other absolute path.

### Running

All classic Gradle building tasks (like `run`, `build`, `-debug-` or `-release-`) work as expected. The only important thing is that on different platforms these tasks are named differently.

```bash
# on Linux:
./gradlew runReleaseExecutableLinuxX64

# on macOS:
./gradlew runReleaseExecutableMacosX64
# or, if Arm architecture is supported:
./gradlew runReleaseExecutableMacosArm64 -Parm # don't forget the -Parm flag!
```

Substituting `Release` with `Debug` disables compiler `-opt` flag.

Also, it is possible to build the project manually with Kotlin CLI compiler. You'd have to either declare several opt-ins or edit the code to remove `expect/actual` and C interop parts. There aren't many benefits to manual compilation, but it allows at least some way to read the program's LLVM IR bitcode (using `-Xtemporary-files-dir` compiler flag and then converting the `.bc` file into readable text with `llvm-dis`).

On MacOS, you can omit the `-Parm` parameter to get an x86 executable.

## Explanation

This tool runs custom-defined litmus tests with various parameters. It is based on techniques used in herdtools/litmus7 and JCStress. 

A single test consists of the following parts:
* A state shared between threads
* Code for each thread
* An outcome &mdash; a certain value which is the result of running the test
* A list of accepted and forbidden outcomes

The tool allocates a batch of shared state instances and runs the threads for each defined thread on one state instance after another, occasionally synchronizing them with barriers. After all threads finish running, states are converted into outcomes, and the same outcomes are counted. The end result is the list of all different observed outcomes, their frequencies and their types (accepted, interesting or forbidden).

### Test syntax

To declare a test, `litmusTest(stateProducer, setup)` function is used. Its first argument is a lambda that produces the shared state instances. The second argument is a lambda used to declare other parts of the test. Here is an example:

```kotlin
class SBState(
  var x: Int = 0,
  var y: Int = 0,
  var r1: Int = 0,
  var r2: Int = 0,
)

val SB: LTDefinition<SBState> = litmusTest(::SBState) {
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

The `thread {}` and `outcome {}` functions have the state as scope receiver. The code is pretty much self-explanatory. There are a few extra convenience features:
* There are a couple of instances of `AutoOutcome` interface, for example, `IIOutcome` with `II` standing for "int, int". If the state extends this class, it will have two extra fields `var r1: Int = 0` and `var r2: Int = 0`. These fields will then automatically be used to form an outcome `listOf(r1, r2)`, so you don't need to a) declare fields for outcome yourself b) declare the `outcome {}` section.
* If the outcome is a `List`, you can use a shorter syntax for declaring accepted / interesting / forbidden outcomes. Just use `accept(vararg outcome)` counterparts to specify expected `List` elements.
* Since each test has its own state, it is quite useful to use anonymous classes for them.

Using these features, the test from above can be shortened a little:
```kotlin
val SB: LTDefinition<*> = litmusTest({
    object : IIOutcome() {
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
    // no need for explicit outcome{}
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
        interesting(0, 0)
    }
}
```

### Entry point

Since this project does not use and kind of reflection or annotation processing API, the new tests have to be manually called and compiled with the project. For this reason, currently there is no CLI or any other interface, you have to modify the `main()` function. Depending on the target, the one in `src/nativeMain/` or `src/jvmMain/` should be used. **Current `main()` functions are the intended way of running any particular test.** You can just substitute the test you want to run and leave the rest as is. There is an option to run the tests with `@Test` annotation with default parameters, but they are run in debug mode by the `kotlinx.test` framework, which is really limiting. 

The tests are run with an `LTRunner`. Currently, there are two implementations: `WorkerRunner` for Kotlin/Native and `JvmThreadRunner` for Kotlin/JVM. The JVM runner is very unreliable because results obtained from it are usually not reproducible. In the future it should be replaced with jcstress interop runner. Any `LTRunner` has several running functions: 
* `runTest(params, test)` simply runs the test one time with the given parameters
* `runTest(duration, params, test)` repeats the test with the same parameters while the given duration lasts
* `runTestParallel(instances, ...)` has the same variants, but it runs several instances of the test in parallel (see code comments on how the affinity map is treated in this case)
* `runTestParallel(...)` without explicit instances number will run `# of cpu cores / # of threads in one test` instances


### Parameters
* `AffinityMap`: bindings from thread to CPU cores. Obtained through `AffinityManager`, which is available from `getAffinityManager()` top-level function.
* `syncEvery`: the number of tests between barrier synchronizations. Practice shows that on Native the reasonable range is somewhere from 10 to 100, while on JVM it works best from 1000 to 10000. This also depends on the particular test.
* ~~`MemShuffler`~~: this is an attempt to shuffle memory accesses in order to prevent false sharing. In practice, the current approach introduces so much overhead that it significantly worsens the results. It's advised to ignore it for now.
* `Barrier`: can be either Kotlin-implemented (`KNativeSpinBarrier`) or C-implemented (`CinteropSpinBarrier`). C-implemented might yield better results. On JVM, use `JvmSpinBarrier` in favor of `JvmCyclicBarrier`.

Common practice is to iterate through different parameters and aggregate the results across many of them: 
* For easy iterating, `variateParameters()` simply takes the cross product of all passed parameters (hence use `listOf(null)` instead of `emptyList()` for unused arguments)
* For aggregating, use `List<LTResult>.mergeResults()`
* You can use `LTResult.prettyPrint()` to print the results, or you can use the results for some automated analysis

## Notes

* Setting thread affinity is not supported on macOS (yet?). As such, `getAffinityManager()` returns `null` on macOS.
* For some reason, running a lot of different tests in one go will drastically reduce the performance and weak outcomes' frequency. For now, better avoid running for longer than 5 minutes.
* From practice, all cases of currently found weak behaviors can be consistently found in under a minute of running.
* Avoid creating objects inside threads, especially if they get shared. This not only significantly slows down the performance, but can also introduce unexpected weak behaviors.
