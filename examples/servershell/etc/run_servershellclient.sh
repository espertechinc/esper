#!/bin/sh

# Script to run JMS server shell client
#

. ./setenv.sh

MEMORY_OPTIONS="-Xms256m"

$JAVA_HOME/bin/java $MEMORY_OPTIONS -Dlog4j.configuration=log4j.xml -cp ${CLASSPATH} com.espertech.esper.example.servershellclient.ServerShellClientMain
