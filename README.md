# Litmus testing on Kotlin/Native

This project is a work-in-progress on a tool for running litmus tests on Kotlin/Native, potentially targeting JVM in the future as well.

**Note**: everything here is experimental, **arbitrary** changes may occur at any moment.

The `main` branch contains some _user-friendlier_ snapshot of the project. See `development` branch for the most recent version.

## Setup

Clone the project.
* On MacOS, this is enough. 
* On Linux, after cloning go to `src/nativeInterop` directory and run the `setup.sh` script. It compiles some C source files and sets up C interop. By default, it outputs the object files to `/usr/...`, which may require `sudo`. You can change the location to any other to avoid `sudo`, but it must be an absolute path.

### Running

```bash
./gradlew runReleaseExecutableLinux
# or
./gradlew runReleaseExecutableMacos -Parm # for Arm-compatible chips 
```

Substituting `Release` with `Debug` disables compiler `-opt` flag. Also, it is possible to simply `./gradlew build` the project and find the executables in `./build/bin/...`

On MacOS, you can omit the `-Parm` parameter to get an x86 executable.

## Explanation

This tool runs custom-defined litmus tests with various parameters. It is based on techniques used in herdtools/litmus7 and JCStress. 

A batch of tests is allocated and then run sequentially, with different threads executing `actorN()` functions in parallel. To prevent one thread consistently running ahead of another, the threads are occasionally synchronized with a barrier.  

Main function currently shows an example of how to use this project. To write a test, you need to extend the `BasicLitmusTest` class. Premade tests in `src/commonMain/kotlin/tests` can be used as examples. To run a test, use the `WorkerTestRunner` class. The results can be printed with `List<LitmusResult>.prettyPrint()` function.

### Parameters
* `AffinityMap`: bindings from thread to CPU cores. Obtained through `AffinityManager`.
* `syncEvery`: the number of tests between barrier synchronizations. Practice shows the values that work best are surprisingly small, from 3 to 10, but any value can be set.
* `MemShuffler`: this is an attempt to shuffle memory accesses in order to prevent false sharing. In practice, the current approach introduces so much overhead that it significantly worsens the results. It's advised to ignore it for now.
* `Barrier`: can be either Kotlin-implemented (`SpinBarrier`) or C-implemented (`CinteropBarrier`). C-implemented might yield better results.
* `run / runParallel`: it is possible to run the same test in parallel: for example, a test that requires 2 threads can be run on a 12-logical-core processor in 6 instances. Note that some of the affinity maps will produce weird results like putting all 12 threads on 2 cores.
* `variateParameters()` simply takes the cross product of all passed parameters. 

## Notes

* Setting thread affinity is not supported on MacOS (yet?). As such, `getAffinityManager()` returns `null` on MacOS. 
* Currently, there is no CLI or any other defined interface. The `main()` function is quite messy and likely will remain so (at least for now).
* For some reason, running a lot of different tests in one go will drastically reduce the performance and weak outcomes frequency. For now, better avoid running for longer than 5 minutes.
* All cases of currently found weak behaviors can be consistently found in under a minute. 
