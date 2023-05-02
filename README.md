# Litmus testing on Kotlin/Native

This project is a work-in-progress on a tool for running litmus tests on Kotlin/Native, potentially targeting JVM in the future as well.

Currently the tool is not ready for usage, though its runner already has most of the planned features. See `development` branch for the most recent version.

### Setup
```bash
cd src/nativeInterop
sudo ./setup.sh
cd ../..
./gradlew runReleaseExecutableXXX # with XXX being Linux or Macos
```

### Notes
~~Currently only GNU-based systems are supported due to the use of `pthread_setaffinity_np`.~~ Both Linux and MacOS are supported, though setting thread affinity is currently only possible on Linux via `pthread`.   

Main function is a bit messy now, but the key function is `runner.runTest(...)`.
