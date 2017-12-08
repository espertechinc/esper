@echo off

if "%JAVA_HOME%" == "" (
  echo.
  echo JAVA_HOME not set
  goto EOF
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo.
  echo Cannot find java executable, check JAVA_HOME
  goto EOF
)

set LIB=..\..\lib

if not exist ..\..\..\esper-7.1.0.jar goto badenv_esperlib
if not exist %LIB%\cglib-nodep-3.2.5.jar goto badenv
if not exist %LIB%\slf4j-api-1.7.25.jar goto badenv
if not exist %LIB%\slf4j-log4j12-1.7.25.jar goto badenv
if not exist %LIB%\log4j-1.2.17.jar goto badenv
if not exist %LIB%\antlr-runtime-4.7.jar goto badenv

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;..\terminalsvc-receiver\target\example-terminalsvc-receiver-1.0.jar
set CLASSPATH=%CLASSPATH%;..\terminalsvc-sender\target\example-terminalsvc-sender-1.0.jar
set CLASSPATH=%CLASSPATH%;..\terminalsvc-common\target\example-terminalsvc-common-1.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-7.1.0.jar
set CLASSPATH=%CLASSPATH%;..\lib\jboss-jms-api_1.1_spec-1.0.0.Final.jar
set CLASSPATH=%CLASSPATH%;..\lib\jboss-client.jar
set CLASSPATH=%CLASSPATH%;%LIB%\cglib-nodep-3.2.5.jar
set CLASSPATH=%CLASSPATH%;%LIB%\slf4j-api-1.7.25.jar.jar
set CLASSPATH=%CLASSPATH%;%LIB%\slf4j-log4j12-1.7.25.jar
set CLASSPATH=%CLASSPATH%;%LIB%\log4j-1.2.17.jar
set CLASSPATH=%CLASSPATH%;%LIB%\antlr-runtime-4.7.jar

goto EOF

:badenv
echo.
echo Error: required libraries not found in %LIB% directory
goto EOF

:badenv_esperlib
echo.
echo Error: esper-7.1.0.jar not found in ..\..\ 

:EOF
