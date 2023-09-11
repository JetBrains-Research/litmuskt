#!/bin/bash

PATH_TO_LIB_BINARY=~/.komem-litmus/kaffinity_gnu.o
mkdir -p ~/.komem-litmus/

gcc kaffinity_gnu.c -c -o $PATH_TO_LIB_BINARY
echo "linkerOpts.linux = $PATH_TO_LIB_BINARY" > kaffinity.def
