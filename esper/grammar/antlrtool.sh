#!/bin/sh

# Script to the ANTLR tool parser compiler
#
java -classpath ../lib/antlr-4.7-complete.jar org.antlr.v4.Tool -o ../src/main/java/com/espertech/esper/epl/generated EsperEPL2Grammar.g

