#!/bin/bash
echo "================================================"
echo " Apple Math Puzzle - Build and Run"
echo "================================================"

SRC="src"
OUT="out"
MAIN="applegame.Main"

mkdir -p "$OUT"

echo "Compiling Java sources..."
javac -encoding UTF-8 -sourcepath "$SRC" -d "$OUT" "$SRC"/applegame/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed. Make sure JDK 11+ is installed."
    exit 1
fi

echo ""
echo "Compilation successful!"
echo "Starting game..."
echo ""
java -Dfile.encoding=UTF-8 -cp "$OUT" $MAIN
