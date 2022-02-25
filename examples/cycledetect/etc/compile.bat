@echo off

call setenv.bat

if not exist "..\target" (
  mkdir ..\target
)
if not exist "..\target\classes" (
  mkdir ..\target\classes
)

set SOURCEPATH=..\src\main\java
set EXAMPLESOURCEPATH=%SOURCEPATH%\com\espertech\esper\example\cycledetect

"%JAVA_HOME%"\bin\javac -d ..\target\classes -source 1.8 -sourcepath %SOURCEPATH% %EXAMPLESOURCEPATH%\CycleDetectMain.java %EXAMPLESOURCEPATH%\CycleDetectorAggregationStateFactory.java %EXAMPLESOURCEPATH%\CycleDetectorAggregationAccessorOutputFactory.java %EXAMPLESOURCEPATH%\CycleDetectorAggregationAccessorDetectFactory.java
