@echo off

REM Script to run named window query example and benchmark
REM

call setenv.bat

set MEMORY_OPTIONS=-Xms1024m -Xmx1024m -server -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java -Dcom.sun.management.jmxremote %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.namedwinquery.NamedWindowQueryMain %1 %2 %3