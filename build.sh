#!/usr/bin/env sh
# Compila o projeto e gera dist/arvores-balanceadas.jar (requer JDK 17 ou superior).
set -e
cd "$(dirname "$0")"

rm -rf build/classes
mkdir -p build/classes dist

# A partir de app/Main.java o javac encontra (via -sourcepath) todas as demais classes.
javac -encoding UTF-8 --release 17 -sourcepath src -d build/classes src/app/Main.java
jar --create --file dist/arvores-balanceadas.jar --main-class app.Main -C build/classes .

echo "Compilado: dist/arvores-balanceadas.jar"
echo "Executar:  java -jar dist/arvores-balanceadas.jar"
