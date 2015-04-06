#!/bin/sh

# Script to run terminal server event sender demonstrating use with J2EE/JBoss
#

. setenv.sh

MEMORY_OPTIONS="-Xms16m -Xmx16m"

$JAVA_HOME/bin/java $MEMORY_OPTIONS -Dlog4j.configuration=log4j.xml -cp ${CLASSPATH} com.espertech.esper.example.terminal.sender.TerminalEventSender $1 $2
