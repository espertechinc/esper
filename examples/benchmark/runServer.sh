# uncomment and set your JAVA_HOME
#JAVA_HOME=""

# the classpath
# you need to get an Esper distribution separately from the benchmark kit
LCP=../../esper/target/classes:target/classes:../../esper/lib/slf4j-log4j12-1.7.25.jar:../../esper/lib/slf4j-api-1.7.25.jar:../../esper/lib/cglib-nodep-3.2.5.jar:../../esper/lib/antlr-runtime-4.7.jar:../../esper/lib/log4j-1.2.17.jar
CP="etc:bin:$LCP:lib/esper-7.1.0.jar:lib/esper_examples_benchmark-7.1.0.jar:lib/slf4j-log4j12-1.7.25.jar:lib/slf4j-api-1.7.25.jar:lib/cglib-nodep-3.2.5.jar:lib/antlr-runtime-4.7.jar:lib/log4j-1.2.17.jar"

# JVM options
OPT="-Xms1024m -Xmx1024m"

# uncomment for simulation without client
#SIM="-rate 2x10000"

# we default to synchronous control flow
QUEUE="-queue -1"

# JMX - if available
CP=$CP:lib/esperjmx-1.0.0.jar
set OPT=$OPT -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false 


$JAVA_HOME/bin/java $OPT -classpath $CP -Desper.benchmark.symbol=1000 com.espertech.esper.example.benchmark.server.Server $QUEUE -stat 10 -mode STP $SIM 2>&1 > out.log
