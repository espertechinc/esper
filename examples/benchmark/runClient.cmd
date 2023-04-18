@echo OFF
setlocal

@rem # uncomment and set your JAVA_HOME
@rem set JAVA_HOME=
set PATH=%JAVA_HOME%\bin;%PATH%

@rem # the classpath
@rem # you need to get an Esper distribution separately from the benchmark kit
set LCP=..\..\esper\target\classes;target\classes;..\..\esper\lib\slf4j-api-1.7.36.jar;..\..\esper\lib\slf4j-reload4j-1.7.36.jar;..\..\esper\lib\antlr4-runtime-4.9.3.jar;..\..\esper\lib\reload4j-1.2.19.jar
set CP=etc;bin;%LCP%;lib\esper-common-8.9.0.jar;lib\esper-compiler-8.9.0.jar;lib\esper-runtime-8.9.0.jar;lib\esper_examples_benchmark-8.9.0.jar;lib\slf4j-reload4j-1.7.36.jar;lib\slf4j-api-1.7.36.jar;lib\antlr4-runtime-4.9.3.jar;lib\reload4j-1.2.19.jar

@rem # JVM options
set OPT=-Xms128m -Xmx128m

@rem # rate
set RATE=-rate 10000

@rem # remote host, we default to localhost and default port
set HOST=-host 128.9.0.1

%JAVA_HOME%\bin\java %OPT% -classpath %CP% -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.client.Client %RATE% %HOST% 
