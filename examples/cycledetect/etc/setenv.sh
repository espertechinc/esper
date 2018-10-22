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

LIB=../lib
LIB_COMMON=../../../esper/lib-common
LIB_COMPILER=../../../esper/lib-compiler

CLASSPATH=.
CLASSPATH=$CLASSPATH:../target/classes
CLASSPATH=$CLASSPATH:../../../esper-common-8.0.0-beta1.jar
CLASSPATH=$CLASSPATH:../../../esper-compiler-8.0.0-beta1.jar
CLASSPATH=$CLASSPATH:../../../esper-runtime-8.0.0-beta1.jar
CLASSPATH=$CLASSPATH:$LIB_COMMON/slf4j-api-1.7.25.jar
CLASSPATH=$CLASSPATH:$LIB_COMMON/slf4j-log4j12-1.7.25.jar
CLASSPATH=$CLASSPATH:$LIB_COMMON/log4j-1.2.17.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/antlr-runtime-4.7.1.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/commons-compiler-3.0.10.jar
CLASSPATH=$CLASSPATH:$LIB_COMPILER/janino-3.0.10.jar
CLASSPATH=$CLASSPATH:$LIB/jgrapht-0.8.3.jar

export CLASSPATH="$CLASSPATH"
