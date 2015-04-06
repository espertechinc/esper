@echo off

REM Script to run quality of service example
REM

call setenv.bat

set MEMORY_OPTIONS=-Xms256m -Xmx256m -XX:+UseParNewGC

"%JAVA_HOME%"\bin\java %MEMORY_OPTIONS% -Dlog4j.configuration=log4j.xml com.espertech.esper.example.qos_sla.QualityOfServiceMain %1 %2