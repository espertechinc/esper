/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.variable;

import com.espertech.esper.core.start.EPStatementStartMethod;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Callable;

public class VariableServiceCallable implements Callable {
    private static final Logger log = LoggerFactory.getLogger(VariableServiceCallable.class);
    private final Random random;
    private final String[] variables;
    private final VariableReader[] readers;
    private final VariableService variableService;
    private final VariableVersionCoord variableVersionCoord;
    private final int numLoops;
    private final int[][] results;
    private final int[] marks;

    public VariableServiceCallable(String[] variables, VariableService variableService, VariableVersionCoord variableVersionCoord, int numLoops) {
        this.random = new Random();
        this.variables = variables;
        this.variableService = variableService;
        this.variableVersionCoord = variableVersionCoord;
        this.numLoops = numLoops;

        results = new int[numLoops][variables.length];
        marks = new int[numLoops];

        readers = new VariableReader[variables.length];
        for (int i = 0; i < variables.length; i++) {
            readers[i] = variableService.getReader(variables[i], EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        }
    }

    public Object call() {
        // For each loop
        for (int i = 0; i < numLoops; i++) {
            doLoop(i);
        }

        return true; // assertions therefore return a result that fails the test
    }

    private void doLoop(int loopNumber) {
        // Set a mark, there should be no number above that number
        int mark = variableVersionCoord.setVersionGetMark();
        int[] indexes = getIndexesShuffled(variables.length, random, loopNumber);
        marks[loopNumber] = mark;

        // Perform first read of all variables
        int[] readResults = new int[variables.length];
        readAll(indexes, readResults, mark);

        // Start a write cycle for the write we are getting an exclusive write lock
        variableService.getReadWriteLock().writeLock().lock();

        // Write every second of the variables
        for (int i = 0; i < indexes.length; i++) {
            int variableNum = indexes[i];
            String variableName = variables[variableNum];

            if (i % 2 == 0) {
                int newMark = variableVersionCoord.incMark();
                if (log.isDebugEnabled()) {
                    log.debug(".run Thread " + Thread.currentThread().getId() + " at mark " + mark + " write variable '" + variableName + "' new value " + newMark);
                }
                variableService.write(readers[variableNum].getVariableMetaData().getVariableNumber(), EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID, newMark);
            }
        }

        // Commit (apply) the changes and unlock
        variableService.commit();
        variableService.getReadWriteLock().writeLock().unlock();

        // Read again and compare to first result
        results[loopNumber] = new int[variables.length];
        readAll(indexes, results[loopNumber], mark);

        // compare first read with second read, written values are NOT visible
        for (int i = 0; i < variables.length; i++) {
            if (results[loopNumber][i] != readResults[i]) {
                String text = "Error in loop#" + loopNumber +
                        " comparing a re-read result for variable " + variables[i] +
                        " expected " + readResults[i] +
                        " but was " + results[loopNumber][i];
                Assert.fail(text);
            }
        }
    }

    private void readAll(int[] indexes, int[] results, int mark) {
        for (int j = 0; j < indexes.length; j++) {
            int index = indexes[j];
            String variableName = variables[index];
            Integer value = (Integer) readers[index].getValue();
            results[index] = value;

            if (log.isDebugEnabled()) {
                log.debug(".run Thread " + Thread.currentThread().getId() + " at mark " + mark + " read variable '" + variableName + " value " + value);
            }
        }
    }

    public int[][] getResults() {
        return results;
    }

    public int[] getMarks() {
        return marks;
    }

    // Make a list between 0 and N for each variable
    private static int[] getIndexes(int length, Random random, int loopNum) {
        int[] indexRandomized = new int[length];

        for (int i = 0; i < indexRandomized.length; i++) {
            indexRandomized[i] = i;
        }

        return indexRandomized;
    }

    // Make a list between 0 and N for each variable
    private static int[] getIndexesShifting(int length, Random random, int loopNum) {
        int[] indexRandomized = new int[length];

        int start = loopNum % length;
        int count = 0;
        for (int i = start; i < indexRandomized.length; i++) {
            indexRandomized[count++] = i;
        }
        for (int i = 0; i < start; i++) {
            indexRandomized[count++] = i;
        }

        return indexRandomized;
    }

    // Make a random list between 0 and N for each variable
    private static int[] getIndexesShuffled(int length, Random random, int loopNum) {
        int[] indexRandomized = new int[length];

        for (int i = 0; i < indexRandomized.length; i++) {
            indexRandomized[i] = i;
        }

        for (int i = 0; i < length; i++) {
            int indexOne = random.nextInt(length);
            int indexTwo = random.nextInt(length);
            int temp = indexRandomized[indexOne];
            indexRandomized[indexOne] = indexRandomized[indexTwo];
            indexRandomized[indexTwo] = temp;
        }

        return indexRandomized;
    }
}
