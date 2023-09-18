#!/bin/bash

PATH_TO_LIB_BINARY=~/.litmuskt/kaffinity_gnu.o
mkdir -p ~/.litmuskt/

gcc kaffinity_gnu.c -c -o $PATH_TO_LIB_BINARY
echo "linkerOpts.linux = $PATH_TO_LIB_BINARY" > kaffinity.def
