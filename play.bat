@echo off
rem Builds (if needed) and launches the game
if not exist PubSimulator.jar call build.bat
start "" javaw -jar PubSimulator.jar
