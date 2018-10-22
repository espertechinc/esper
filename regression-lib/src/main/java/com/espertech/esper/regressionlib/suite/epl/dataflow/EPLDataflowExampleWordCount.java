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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.dataflow.MyLineFeedSource;
import com.espertech.esper.regressionlib.support.dataflow.MyWordCountStats;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class EPLDataflowExampleWordCount implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String epl = "@name('flow') create dataflow WordCount " +
            "MyLineFeedSource -> LineOfTextStream {} " +
            "MyTokenizerCounter(LineOfTextStream) -> SingleLineCountStream {}" +
            "MyWordCountAggregator(SingleLineCountStream) -> WordCountStream {}" +
            "DefaultSupportCaptureOp(WordCountStream) {}";
        env.compileDeploy(epl);

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        MyLineFeedSource source = new MyLineFeedSource(Arrays.asList("Test this code", "Test line two").iterator());

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future, source));

        env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "WordCount", options).start();

        Object[] received = new Object[0];
        try {
            received = future.get(3, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(1, received.length);
        MyWordCountStats stats = (MyWordCountStats) received[0];
        assertEquals(2, stats.getLines());
        assertEquals(6, stats.getWords());
        assertEquals(23, stats.getChars());

        env.undeployAll();
    }
}
