#!/bin/bash

INTEROP_FOLDER=$1

BINARY_PATH="$INTEROP_FOLDER/kaffinity_gnu.o"
DEF_FILE_PATH="$INTEROP_FOLDER/kaffinity.def"
SOURCE_PATH="$INTEROP_FOLDER/kaffinity_gnu.c"

gcc "$SOURCE_PATH" -c -o "$BINARY_PATH"
echo "linkerOpts.linux = $BINARY_PATH" > "$DEF_FILE_PATH"
