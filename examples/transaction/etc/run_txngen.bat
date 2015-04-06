@echo off

REM Script to run transaction generator
REM
REM change log4j.xml logger configuration file to DEBUG for more detailed output or INFO for less detailed output

call setenv.bat

set MEMORY_OPTIONS=-Xms256m -Xmx256m -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.transaction.sim.TxnGenMain %1 %2