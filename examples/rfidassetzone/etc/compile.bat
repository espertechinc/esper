@echo off

call setenv.bat

if not exist "..\target" (
  mkdir ..\target
)
if not exist "..\target\classes" (
  mkdir ..\target\classes
)

set SOURCEPATH=..\src\main\java

"%JAVA_HOME%"\bin\javac -d ..\target\classes -source 1.6 -sourcepath %SOURCEPATH% %SOURCEPATH%\com\espertech\esper\example\rfidassetzone\RFIDMouseDragExample.java %SOURCEPATH%\com\espertech\esper\example\rfidassetzone\LRMovingSimMain.java
