@echo off

REM Script to run market data feed example
REM

call setenv.bat

set MEMORY_OPTIONS=-Xms512m -Xmx512m -server -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java -Dcom.sun.management.jmxremote %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.marketdatafeed.FeedSimMain %1 %2 %3