#!/bin/sh

## run via '. setenv.sh'
##
##

if [ -z "${JAVA_HOME}" ]
then
  echo "JAVA_HOME not set"
  exit 0
fi

if [ ! -x "${JAVA_HOME}/bin/java" ]
then
  echo Cannot find java executable, check JAVA_HOME
  exit 0
fi

LIB=../../../esper/lib
EXLIB=../../../examples/lib
IOLIB=../../../esperio/lib

CLASSPATH=.
CLASSPATH=$CLASSPATH:../target/classes
CLASSPATH=$CLASSPATH:../../../esper-6.0.0.jar
CLASSPATH=$CLASSPATH:$LIB/cglib-nodep-3.2.4.jar
CLASSPATH=$CLASSPATH:$LIB/commons-logging-1.1.3.jar
CLASSPATH=$CLASSPATH:$LIB/log4j-1.2.17.jar
CLASSPATH=$CLASSPATH:$LIB/antlr-runtime-4.5.3.jar
CLASSPATH=$CLASSPATH:$EXLIB/jms.jar
CLASSPATH=$CLASSPATH:$IOLIB/apache-activemq-6.0.0-incubator.jar

export CLASSPATH="$CLASSPATH"
