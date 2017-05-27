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
package com.espertech.esper.regression.dataflow;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteDataflow extends TestCase {
    public void testExecDataflowAPIConfigAndInstance() {
        RegressionRunner.run(new ExecDataflowAPIConfigAndInstance());
    }

    public void testExecDataflowAPICreateStartStopDestroy() {
        RegressionRunner.run(new ExecDataflowAPICreateStartStopDestroy());
    }

    public void testExecDataflowAPIExceptions() {
        RegressionRunner.run(new ExecDataflowAPIExceptions());
    }

    public void testExecDataflowAPIInstantiationOptions() {
        RegressionRunner.run(new ExecDataflowAPIInstantiationOptions());
    }

    public void testExecDataflowAPIOpLifecycle() {
        RegressionRunner.run(new ExecDataflowAPIOpLifecycle());
    }

    public void testExecDataflowAPIRunStartCancelJoin() {
        RegressionRunner.run(new ExecDataflowAPIRunStartCancelJoin());
    }

    public void testExecDataflowAPIStartCaptive() {
        RegressionRunner.run(new ExecDataflowAPIStartCaptive());
    }

    public void testExecDataflowAPIStatistics() {
        RegressionRunner.run(new ExecDataflowAPIStatistics());
    }

    public void testExecDataflowCustomProperties() {
        RegressionRunner.run(new ExecDataflowCustomProperties());
    }

    public void testExecDataflowOpBeaconSource() {
        RegressionRunner.run(new ExecDataflowOpBeaconSource());
    }

    public void testExecDataflowOpEPStatementSource() {
        RegressionRunner.run(new ExecDataflowOpEPStatementSource());
    }

    public void testExecDataflowOpEventBusSink() {
        RegressionRunner.run(new ExecDataflowOpEventBusSink());
    }

    public void testExecDataflowOpEventBusSource() {
        RegressionRunner.run(new ExecDataflowOpEventBusSource());
    }

    public void testExecDataflowOpFilter() {
        RegressionRunner.run(new ExecDataflowOpFilter());
    }

    public void testExecDataflowOpLogSink() {
        RegressionRunner.run(new ExecDataflowOpLogSink());
    }

    public void testExecDataflowOpSelect() {
        RegressionRunner.run(new ExecDataflowOpSelect());
    }

    public void testExecDataflowDocSamples() {
        RegressionRunner.run(new ExecDataflowDocSamples());
    }

    public void testExecDataflowExampleRollingTopWords() {
        RegressionRunner.run(new ExecDataflowExampleRollingTopWords());
    }

    public void testExecDataflowExampleVwapFilterSelectJoin() {
        RegressionRunner.run(new ExecDataflowExampleVwapFilterSelectJoin());
    }

    public void testExecDataflowExampleWordCount() {
        RegressionRunner.run(new ExecDataflowExampleWordCount());
    }

    public void testExecDataflowInputOutputVariations() {
        RegressionRunner.run(new ExecDataflowInputOutputVariations());
    }

    public void testExecDataflowInvalidGraph() {
        RegressionRunner.run(new ExecDataflowInvalidGraph());
    }

    public void testExecDataflowTypes() {
        RegressionRunner.run(new ExecDataflowTypes());
    }
}
