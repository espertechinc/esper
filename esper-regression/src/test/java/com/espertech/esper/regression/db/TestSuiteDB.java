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
package com.espertech.esper.regression.db;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteDB extends TestCase {
    public void testExecDatabase2StreamOuterJoin() {
        RegressionRunner.run(new ExecDatabase2StreamOuterJoin());
    }

    public void testExecDatabase3StreamOuterJoin() {
        RegressionRunner.run(new ExecDatabase3StreamOuterJoin());
    }

    public void testExecDatabaseDataSourceFactory() {
        RegressionRunner.run(new ExecDatabaseDataSourceFactory());
    }

    public void testExecDatabaseHintHook() {
        RegressionRunner.run(new ExecDatabaseHintHook());
    }

    public void testExecDatabaseJoin() {
        RegressionRunner.run(new ExecDatabaseJoin());
    }

    public void testExecDatabaseJoinInsertInto() {
        RegressionRunner.run(new ExecDatabaseJoinInsertInto());
    }

    public void testExecDatabaseJoinOptions() {
        RegressionRunner.run(new ExecDatabaseJoinOptions());
    }

    public void testExecDatabaseJoinOptionUppercase() {
        RegressionRunner.run(new ExecDatabaseJoinOptionUppercase());
    }

    public void testExecDatabaseJoinOptionLowercase() {
        RegressionRunner.run(new ExecDatabaseJoinOptionLowercase());
    }

    public void testExecDatabaseJoinPerfNoCache() {
        RegressionRunner.run(new ExecDatabaseJoinPerfNoCache());
    }

    public void testExecDatabaseJoinPerfWithCache() {
        RegressionRunner.run(new ExecDatabaseJoinPerfWithCache());
    }

    public void testExecDatabaseNoJoinIterate() {
        RegressionRunner.run(new ExecDatabaseNoJoinIterate());
    }

    public void testExecDatabaseNoJoinIteratePerf() {
        RegressionRunner.run(new ExecDatabaseNoJoinIteratePerf());
    }

    public void testExecDatabaseOuterJoinWCache() {
        RegressionRunner.run(new ExecDatabaseOuterJoinWCache());
    }

    public void testExecDatabaseQueryResultCache() {
        RegressionRunner.run(new ExecDatabaseQueryResultCache(false, null, 1d, Double.MAX_VALUE, 5000L, 1000, false));
        RegressionRunner.run(new ExecDatabaseQueryResultCache(true, 100, null, null, 2000L, 1000, false));
        RegressionRunner.run(new ExecDatabaseQueryResultCache(true, 100, null, null, 7000L, 25000, false));
        RegressionRunner.run(new ExecDatabaseQueryResultCache(false, null, 2d, 2d, 7000L, 25000, false));
        RegressionRunner.run(new ExecDatabaseQueryResultCache(false, null, 1d, 1d, 7000L, 25000, true));
    }
}
