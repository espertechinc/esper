# uncomment and set your JAVA_HOME
#JAVA_HOME=""

# the classpath
# you need to get an Esper distribution separately from the benchmark kit
LCP=../../esper/target/classes:target/classes:../../esper/lib/slf4j-log4j12-1.7.21.jar:../../esper/lib/slf4j-api-1.7.21.jar:../../esper/lib/cglib-nodep-3.2.4.jar:../../esper/lib/antlr-runtime-4.5.3.jar:../../esper/lib/log4j-1.2.17.jar
CP="etc:bin:$LCP:lib/esper-6.1.0.jar:lib/esper_examples_benchmark-6.1.0.jar:lib/slf4j-api-1.7.21.jar:lib/slf4j-log4j12-1.7.21.jar:lib/cglib-nodep-3.2.4.jar:lib/antlr-runtime-4.5.3.jar:lib/log4j-1.2.17.jar"

# JVM options
OPT="-Xms128m -Xmx128m"

# rate
RATE="-rate 10000"

# remote host, we default to localhost and default port
HOST="-host 127.0.0.1"

$JAVA_HOME/bin/java $OPT -classpath $CP -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.client.Client $RATE $HOST



