#!/bin/sh

# Script to run RFID Swing example
#

# A note to cygwin users: please replace "-cp ${CLASSPATH}" with "-cp `cygpath -wp $CLASSPATH`"
#

. setenv.sh

MEMORY_OPTIONS="-Xms128m -Xmx128m -server -XX:+UseParNewGC"

$JAVA_HOME/bin/java $MEMORY_OPTIONS -Dlog4j.configuration=log4j.xml -cp ${CLASSPATH} com.espertech.esper.example.rfidassetzone.RFIDMouseDragExample $1 $2 $3
