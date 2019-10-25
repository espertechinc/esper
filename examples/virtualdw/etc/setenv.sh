#!/bin/sh

## run via '. ./setenv.sh'
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

LIB_COMPILER=../../../dependencies/compiler

CLASSPATH=.
CLASSPATH=$CLASSPATH:../target/classes
CLASSPATH=$CLASSPATH:../../../esper-common-8.4.0.jar
CLASSPATH=$CLASSPATH:../../../esper-compiler-8.4.0.jar
CLASSPATH=$CLASSPATH:../../../esper-runtime-8.4.0.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/slf4j-api-1.7.26.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/slf4j-log4j12-1.7.26.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/log4j-1.2.17.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/antlr4-runtime-4.7.2.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/commons-compiler-3.1.0.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/janino-3.1.0.jar

export CLASSPATH="$CLASSPATH"
