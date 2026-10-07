#!/bin/sh
# Builds PubSimulator.jar (needs a JDK 17+)
mkdir -p out && javac -encoding UTF-8 -d out src/pub/*.java && jar cfe PubSimulator.jar pub.Main -C out . && echo "Built PubSimulator.jar. Run: java -jar PubSimulator.jar"
