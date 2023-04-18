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

LIB=../lib
LIB_COMPILER=../../../dependencies/compiler

CLASSPATH=.
CLASSPATH=$CLASSPATH:../target/classes
CLASSPATH=$CLASSPATH:../../../esper-common-8.9.0.jar
CLASSPATH=$CLASSPATH:../../../esper-compiler-8.9.0.jar
CLASSPATH=$CLASSPATH:../../../esper-runtime-8.9.0.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/slf4j-api-1.7.36.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/slf4j-reload4j-1.7.36.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/reload4j-1.2.19.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/antlr4-runtime-4.9.3.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/commons-compiler-3.1.6.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/janino-3.1.6.jar
CLASSPATH=$CLASSPATH:$LIB/jgrapht-0.8.3.jar

export CLASSPATH="$CLASSPATH"
