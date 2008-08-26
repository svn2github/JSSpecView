@echo off
rem Set JSV_HOME to the JSpecView installation directory.
rem
if "%JSV_HOME%x"=="x" set JSV_HOME="."
java -Xmx512m -jar "%JSV_HOME%\JSVApp.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9
