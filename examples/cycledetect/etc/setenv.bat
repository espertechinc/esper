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
set LIB_COMPILER=..\..\..\dependencies\compiler

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;..\target\classes
set CLASSPATH=%CLASSPATH%;..\..\..\esper-common-8.9.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-compiler-8.9.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-runtime-8.9.0.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\slf4j-api-1.7.36.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\slf4j-reload4j-1.7.36.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\reload4j-1.2.19.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\antlr4-runtime-4.9.3.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\commons-compiler-3.1.9.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\janino-3.1.9.jar
set CLASSPATH=%CLASSPATH%;%LIB%\jgrapht-0.8.3.jar

:EOF
