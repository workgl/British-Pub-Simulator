@echo off
rem Builds PubSimulator.jar (needs a JDK 17+ on PATH)
if not exist out mkdir out
javac -encoding UTF-8 -d out src\pub\*.java || exit /b 1
jar cfe PubSimulator.jar pub.Main -C out .
echo Built PubSimulator.jar
