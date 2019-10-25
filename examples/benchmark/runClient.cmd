@echo OFF
setlocal

@rem # uncomment and set your JAVA_HOME
@rem set JAVA_HOME=
set PATH=%JAVA_HOME%\bin;%PATH%

@rem # the classpath
@rem # you need to get an Esper distribution separately from the benchmark kit
set LCP=..\..\esper\target\classes;target\classes;..\..\esper\lib\slf4j-api-1.7.26.jar;..\..\esper\lib\slf4j-log4j12-1.7.26.jar;..\..\esper\lib\antlr4-runtime-4.7.2.jar;..\..\esper\lib\log4j-1.2.17.jar
set CP=etc;bin;%LCP%;lib\esper-common-8.4.0.jar;lib\esper-compiler-8.4.0.jar;lib\esper-runtime-8.4.0.jar;lib\esper_examples_benchmark-8.4.0.jar;lib\slf4j-log4j12-1.7.26.jar;lib\slf4j-api-1.7.26.jar;lib\antlr4-runtime-4.7.2.jar;lib\log4j-1.2.17.jar

@rem # JVM options
set OPT=-Xms128m -Xmx128m

@rem # rate
set RATE=-rate 10000

@rem # remote host, we default to localhost and default port
set HOST=-host 128.4.0.1

%JAVA_HOME%\bin\java %OPT% -classpath %CP% -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.client.Client %RATE% %HOST% 
