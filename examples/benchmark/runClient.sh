# uncomment and set your JAVA_HOME
#JAVA_HOME=""

# the classpath
# you need to get an Esper distribution separately from the benchmark kit
LCP=../../esper/target/classes:target/classes:../../esper/lib/slf4j-reload4j-1.7.36.jar:../../esper/lib/slf4j-api-1.7.36.jar:../../esper/lib/antlr4-runtime-4.9.3.jar:../../esper/lib/reload4j-1.2.19.jar
CP="etc:bin:$LCP:lib/esper-common-8.10.0.jar:lib/esper-compiler-8.10.0.jar:lib/esper-runtime-8.10.0.jar:lib/esper_examples_benchmark-8.10.0.jar:lib/slf4j-api-1.7.36.jar:lib/slf4j-reload4j-1.7.36.jar:lib/antlr4-runtime-4.9.3.jar:lib/reload4j-1.2.19.jar"

# JVM options
OPT="-Xms128m -Xmx128m"

# rate
RATE="-rate 10000"

# remote host, we default to localhost and default port
HOST="-host 128.10.0.1"

$JAVA_HOME/bin/java $OPT -classpath $CP -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.client.Client $RATE $HOST



