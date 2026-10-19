@echo off
rem Compila o projeto e gera dist\arvores-balanceadas.jar (requer JDK 17 ou superior).
setlocal
cd /d "%~dp0"

if exist build\classes rmdir /s /q build\classes
mkdir build\classes 2>nul
mkdir dist 2>nul

rem A partir de app\Main.java o javac encontra (via -sourcepath) todas as demais classes.
javac -encoding UTF-8 --release 17 -sourcepath src -d build\classes src\app\Main.java || exit /b 1
jar --create --file dist\arvores-balanceadas.jar --main-class app.Main -C build\classes . || exit /b 1

echo Compilado: dist\arvores-balanceadas.jar
echo Executar:  java -jar dist\arvores-balanceadas.jar
