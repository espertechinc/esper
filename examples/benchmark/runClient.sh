# uncomment and set your JAVA_HOME
#JAVA_HOME=""

# the classpath
# you need to get an Esper distribution separately from the benchmark kit
LCP=../../esper/target/classes:../../esper/lib/commons-logging-1.1.3.jar:../../esper/lib/cglib-nodep-3.1.jar:../../esper/lib/antlr-runtime-4.1.jar:../../esper/lib/log4j-1.2.17.jar
CP="etc:bin:$LCP:lib/esper-5.2.0.jar:lib/commons-logging-1.1.3.jar:lib/cglib-nodep-3.1.jar:lib/antlr-runtime-4.1.jar:lib/log4j-1.2.17.jar"

# JVM options
OPT="-Xms128m -Xmx128m"

# rate
RATE="-rate 10000"

# remote host, we default to localhost and default port
HOST="-host 127.0.0.1"

$JAVA_HOME/bin/java $OPT -classpath $CP -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.client.Client $RATE $HOST



