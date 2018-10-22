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

set LIB=..\lib
set LIB_COMMON=..\..\..\esper\lib-common
set LIB_COMPILER=..\..\..\esper\lib-compiler

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;..\target\classes
set CLASSPATH=%CLASSPATH%;..\..\..\esper-common-8.0.0-beta1.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-compiler-8.0.0-beta1.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-runtime-8.0.0-beta1.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMMON%\slf4j-api-1.7.25.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMMON%\slf4j-log4j12-1.7.25.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMMON%\log4j-1.2.17.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\antlr-runtime-4.7.1.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\commons-compiler-3.0.10.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\janino-3.0.10.jar
set CLASSPATH=%CLASSPATH%;%LIB%\jgrapht-0.8.3.jar

:EOF
