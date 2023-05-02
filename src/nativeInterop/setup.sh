#!/bin/bash

PATH_TO_LIB_BINARY=/usr/include/kaffinity/kaffinity_gnu.o

gcc kaffinity_gnu.c -c -o $PATH_TO_LIB_BINARY
echo "linkerOpts.linux = $PATH_TO_LIB_BINARY" > kaffinity.def
