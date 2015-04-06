#!/bin/sh

# Script to run example
#

# A note to cygwin users: please replace "-cp ${CLASSPATH}" with "-cp `cygpath -wp $CLASSPATH`"
#

. setenv.sh

MEMORY_OPTIONS="-Xms512m -Xmx512m -server -XX:+UseParNewGC"

$JAVA_HOME/bin/java $MEMORY_OPTIONS -Dlog4j.configuration=log4j.xml -cp ${CLASSPATH} com.espertech.esper.example.virtualdw.SampleVirtualDataWindowMain $1 $2 $3
