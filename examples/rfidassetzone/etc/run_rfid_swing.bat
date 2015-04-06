@echo off

REM Script to run RFID Swing example
REM

call setenv.bat

set MEMORY_OPTIONS=-Xms128m -Xmx128m -server -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java -Dcom.sun.management.jmxremote %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.rfidassetzone.RFIDMouseDragExample %1 %2 %3