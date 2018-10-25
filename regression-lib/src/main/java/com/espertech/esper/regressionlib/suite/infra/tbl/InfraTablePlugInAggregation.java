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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTablePlugInAggregation {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraPlugInAggMethodCSVLast3Strings());
        execs.add(new InfraPlugInAccessRefCountedMap());
        return execs;
    }

    // CSV-building over a limited set of values.
    //
    // Use aggregation method single-value when the aggregation has a natural current value
    // that can be obtained without asking it a question.
    private static class InfraPlugInAggMethodCSVLast3Strings implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggPIN (csv csvWords(string))", path);
            env.compileDeploy("@name('s0') select varaggPIN.csv as c0 from SupportBean_S0", path).addListener("s0");
            env.compileDeploy("into table varaggPIN select csvWords(theString) as csv from SupportBean#length(3)", path);

            sendWordAssert(env, "the", "the");
            sendWordAssert(env, "fox", "the,fox");
            sendWordAssert(env, "jumps", "the,fox,jumps");
            sendWordAssert(env, "over", "fox,jumps,over");

            env.undeployAll();
        }
    }

    private static class InfraPlugInAccessRefCountedMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table varaggRCM (wordCount referenceCountedMap(string))", path);
            env.compileDeploy("into table varaggRCM select referenceCountedMap(theString) as wordCount from SupportBean#length(3)", path);
            env.compileDeploy("@name('s0') select varaggRCM.wordCount.referenceCountLookup(p00) as c0 from SupportBean_S0", path).addListener("s0");

            String words = "the,house,is,green";
            sendWordAssert(env, "the", words, new Integer[]{1, null, null, null});
            sendWordAssert(env, "house", words, new Integer[]{1, 1, null, null});
            sendWordAssert(env, "the", words, new Integer[]{2, 1, null, null});
            sendWordAssert(env, "green", words, new Integer[]{1, 1, null, 1});
            sendWordAssert(env, "is", words, new Integer[]{1, null, 1, 1});

            env.undeployAll();
        }
    }

    private static void sendWordAssert(RegressionEnvironment env, String word, String expected) {
        env.sendEventBean(new SupportBean(word, 0));
        env.sendEventBean(new SupportBean_S0(0));
        assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
    }

    private static void sendWordAssert(RegressionEnvironment env, String word, String wordCSV, Integer[] counts) {
        env.sendEventBean(new SupportBean(word, 0));

        String[] words = wordCSV.split(",");
        for (int i = 0; i < words.length; i++) {
            env.sendEventBean(new SupportBean_S0(0, words[i]));
            Integer count = (Integer) env.listener("s0").assertOneGetNewAndReset().get("c0");
            assertEquals("failed for word '" + words[i] + "'", counts[i], count);
        }
    }
}


