#!/bin/sh

# Script to run JMS server shell
#

. setenv.sh

MEMORY_OPTIONS="-Xms256m -Xmx256m -XX:+UseParNewGC"

$JAVA_HOME/bin/java $MEMORY_OPTIONS -Dlog4j.configuration=log4j.xml -cp ${CLASSPATH} com.espertech.esper.example.servershell.ServerShellMain
