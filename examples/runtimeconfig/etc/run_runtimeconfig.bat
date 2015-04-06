@echo off

REM Script to run runtime config example
REM

call setenv.bat

set MEMORY_OPTIONS=-Xms1024m -Xmx1024m -server -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java -Dcom.sun.management.jmxremote %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.runtimeconfig.RuntimeConfigMain %1 %2 %3