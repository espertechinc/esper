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
package com.espertech.esper.regression.nwtable.infra;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteInfra extends TestCase {
    public void testExecNWTableInfraComparative() {
        RegressionRunner.run(new ExecNWTableInfraComparative());
    }

    public void testExecNWTableInfraContext() {
        RegressionRunner.run(new ExecNWTableInfraContext());
    }

    public void testExecNWTableInfraCreateIndex() {
        RegressionRunner.run(new ExecNWTableInfraCreateIndex());
    }

    public void testExecNWTableInfraEventType() {
        RegressionRunner.run(new ExecNWTableInfraEventType());
    }

    public void testExecNWTableInfraExecuteQuery() {
        RegressionRunner.run(new ExecNWTableInfraExecuteQuery());
    }

    public void testExecNWTableInfraIndexFAF() {
        RegressionRunner.run(new ExecNWTableInfraIndexFAF());
    }

    public void testExecNWTableInfraIndexFAFPerf() {
        RegressionRunner.run(new ExecNWTableInfraIndexFAFPerf());
    }

    public void testExecNWTableInfraCreateIndexAdvancedSyntax() {
        RegressionRunner.run(new ExecNWTableInfraCreateIndexAdvancedSyntax());
    }

    public void testExecNWTableInfraOnDelete() {
        RegressionRunner.run(new ExecNWTableInfraOnDelete());
    }

    public void testExecNWTableInfraOnMerge() {
        RegressionRunner.run(new ExecNWTableInfraOnMerge());
    }

    public void testExecNWTableInfraOnMergePerf() {
        RegressionRunner.run(new ExecNWTableInfraOnMergePerf());
    }

    public void testExecNWTableInfraOnSelect() {
        RegressionRunner.run(new ExecNWTableInfraOnSelect());
    }

    public void testExecNWTableOnSelectWDelete() {
        RegressionRunner.run(new ExecNWTableOnSelectWDelete());
    }

    public void testExecNWTableInfraOnUpdate() {
        RegressionRunner.run(new ExecNWTableInfraOnUpdate());
    }

    public void testExecNWTableInfraStartStop() {
        RegressionRunner.run(new ExecNWTableInfraStartStop());
    }

    public void testExecNWTableInfraSubqCorrelCoerce() {
        RegressionRunner.run(new ExecNWTableInfraSubqCorrelCoerce());
    }

    public void testExecNWTableInfraSubqCorrelIndex() {
        RegressionRunner.run(new ExecNWTableInfraSubqCorrelIndex());
    }

    public void testExecNWTableInfraSubqCorrelJoin() {
        RegressionRunner.run(new ExecNWTableInfraSubqCorrelJoin());
    }

    public void testExecNWTableInfraSubquery() {
        RegressionRunner.run(new ExecNWTableInfraSubquery());
    }

    public void testExecNWTableInfraSubqueryAtEventBean() {
        RegressionRunner.run(new ExecNWTableInfraSubqueryAtEventBean());
    }

    public void testExecNWTableInfraSubqUncorrel() {
        RegressionRunner.run(new ExecNWTableInfraSubqUncorrel());
    }

}
