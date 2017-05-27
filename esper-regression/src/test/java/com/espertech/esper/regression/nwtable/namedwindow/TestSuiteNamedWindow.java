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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.ConfigurationEngineDefaults;
import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

// see INFRA suite for additional Named Window tests
public class TestSuiteNamedWindow extends TestCase {
    public void testExecNamedWindowConsumer() {
        RegressionRunner.run(new ExecNamedWindowConsumer());
    }

    public void testExecNamedWindowContainedEvent() {
        RegressionRunner.run(new ExecNamedWindowContainedEvent());
    }

    public void testExecNamedWindowIndex() {
        RegressionRunner.run(new ExecNamedWindowIndex());
    }

    public void testExecNamedWindowIndexAddedValType() {
        RegressionRunner.run(new ExecNamedWindowIndexAddedValType());
    }

    public void testExecNamedWindowInsertFrom() {
        RegressionRunner.run(new ExecNamedWindowInsertFrom());
    }

    public void testExecNamedWindowJoin() {
        RegressionRunner.run(new ExecNamedWindowJoin());
    }

    public void testExecNamedWindowOM() {
        RegressionRunner.run(new ExecNamedWindowOM());
    }

    public void testExecNamedWindowOnDelete() {
        RegressionRunner.run(new ExecNamedWindowOnDelete());
    }

    public void testExecNamedWindowOnMerge() {
        RegressionRunner.run(new ExecNamedWindowOnMerge());
    }

    public void testExecNamedWindowOnSelect() {
        RegressionRunner.run(new ExecNamedWindowOnSelect());
    }

    public void testExecNamedWindowOnUpdate() {
        RegressionRunner.run(new ExecNamedWindowOnUpdate());
    }

    public void testExecNamedWindowOnUpdateWMultiDispatch() {
        RegressionRunner.run(new ExecNamedWindowOnUpdateWMultiDispatch(true, null, null));
        RegressionRunner.run(new ExecNamedWindowOnUpdateWMultiDispatch(false, true, ConfigurationEngineDefaults.Threading.Locking.SPIN));
        RegressionRunner.run(new ExecNamedWindowOnUpdateWMultiDispatch(false, true, ConfigurationEngineDefaults.Threading.Locking.SUSPEND));
        RegressionRunner.run(new ExecNamedWindowOnUpdateWMultiDispatch(false, false, null));
    }

    public void testExecNamedWindowOutputrate() {
        RegressionRunner.run(new ExecNamedWindowOutputrate());
    }

    public void testExecNamedWindowProcessingOrder() {
        RegressionRunner.run(new ExecNamedWindowProcessingOrder());
    }

    public void testExecNamedWindowRemoveStream() {
        RegressionRunner.run(new ExecNamedWindowRemoveStream());
    }

    public void testExecNamedWindowStartStop() {
        RegressionRunner.run(new ExecNamedWindowStartStop());
    }

    public void testExecNamedWindowSubquery() {
        RegressionRunner.run(new ExecNamedWindowSubquery());
    }

    public void testExecNamedWindowTypes() {
        RegressionRunner.run(new ExecNamedWindowTypes());
    }

    public void testExecNamedWindowViews() {
        RegressionRunner.run(new ExecNamedWindowViews());
    }

    public void testExecNamedWindowPerformance() {
        RegressionRunner.run(new ExecNamedWindowPerformance());
    }

    public void testExecNamedWIndowFAFQueryJoinPerformance() {
        RegressionRunner.run(new ExecNamedWIndowFAFQueryJoinPerformance());
    }

}
