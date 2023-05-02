# Experiments with running litmus tests

### Setup
```bash
cd src/nativeInterop
sudo ./setup.sh
cd ../..
./gradlew build # or runXxxExecutableNative
```

### Notes
Currently only GNU-based systems are supported due to the use of `pthread_setaffinity_np`. 

Main function is a bit messy now, but the key function is `runner.runTest(...)`.
