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

set LIB_COMPILER=..\..\..\dependencies\compiler
set LIB_COMMON_XMLXSD=..\..\..\dependencies\common-xmlxsd

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;..\target\classes
set CLASSPATH=%CLASSPATH%;..\..\..\esper-common-8.10.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-compiler-8.10.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-runtime-8.10.0.jar
set CLASSPATH=%CLASSPATH%;..\..\..\esper-common-xmlxsd-8.10.0.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\slf4j-api-1.7.36.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\slf4j-reload4j-1.7.36.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\reload4j-1.2.19.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\antlr4-runtime-4.13.1.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\commons-compiler-3.1.9.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMPILER%\janino-3.1.9.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMMON_XMLXSD%\xercesImpl-2.12.2.jar
set CLASSPATH=%CLASSPATH%;%LIB_COMMON_XMLXSD%\xml-apis-1.4.01.jar


:EOF
