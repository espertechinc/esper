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
CLASSPATH=$CLASSPATH:../../../esper-7.1.0.jar
CLASSPATH=$CLASSPATH:$LIB/cglib-nodep-3.2.5.jar
CLASSPATH=$CLASSPATH:$LIB/slf4j-api-1.7.25.jar
CLASSPATH=$CLASSPATH:$LIB/slf4j-log4j12-1.7.25.jar
CLASSPATH=$CLASSPATH:$LIB/log4j-1.2.17.jar
CLASSPATH=$CLASSPATH:$LIB/antlr-runtime-4.7.jar
CLASSPATH=$CLASSPATH:$EXLIB/jms.jar
CLASSPATH=$CLASSPATH:$IOLIB/apache-activemq-7.1.0-incubator.jar

export CLASSPATH="$CLASSPATH"
