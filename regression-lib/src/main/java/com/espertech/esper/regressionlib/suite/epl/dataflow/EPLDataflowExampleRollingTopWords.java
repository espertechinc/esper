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

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEmitterOperator;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class EPLDataflowExampleRollingTopWords implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        if (env.isHA()) {
            return;
        }

        String epl = "@name('flow') create dataflow RollingTopWords\n" +
            "create objectarray schema WordEvent (word string),\n" +
            "Emitter -> wordstream<WordEvent> {name:'a'} // Produces word stream\n" +
            "Select(wordstream) -> wordcount { // Sliding time window count per word\n" +
            "  select: (select word, count(*) as wordcount from wordstream#time(30) group by word)\n" +
            "}\n" +
            "Select(wordcount) -> wordranks { // Rank of words\n" +
            "  select: (select window(*) as rankedWords from wordcount#sort(3, wordcount desc) output snapshot every 2 seconds)\n" +
            "}\n" +
            "DefaultSupportCaptureOp(wordranks) {}";
        env.eventService().advanceTime(0);
        env.compileDeploy(epl);

        // prepare test
        DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(capture));

        EPDataFlowInstance instanceOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "RollingTopWords", options);
        EPDataFlowEmitterOperator emitter = instanceOne.startCaptive().getEmitters().get("a");

        for (String word : new String[]{"this", "is", "a", "test", "that", "is", "a", "word", "test"}) {
            emitter.submit(new Object[]{word});
        }
        assertEquals(0, capture.getCurrentAndReset().length);

        env.advanceTime(2000);
        assertEquals(1, capture.getCurrent().length);
        Object[] row = (Object[]) capture.getCurrent()[0];
        Object[][] rows = (Object[][]) row[0];
        EPAssertionUtil.assertPropsPerRow(rows, "word,count".split(","), new Object[][]{{"is", 2L}, {"a", 2L}, {"test", 2L}});

        instanceOne.cancel();

        env.undeployAll();
    }

    /**
     * Comment-In for online flow-testing.
     * <p>
     * public void testOnline() throws Exception {
     * env.eventService().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
     * <p>
     * String epl = "create dataflow RollingTopWords\n" +
     * "create objectarray schema WordEvent (word string);\n" +
     * "MyWordTestSource -> wordstream<WordEvent> {} // Produces word stream\n" +
     * "Select(wordstream) -> wordcount { // Sliding time window count per word\n" +
     * "  select: select word, count(*) as wordcount from wordstream#time(30) group by word;\n" +
     * "}\n" +
     * "Select(wordcount) -> wordranks { // Rank of words\n" +
     * "  select: select prevwindow(wc) from wordcount#rank(word, 3, wordcount desc) as wc output snapshot every 2 seconds limit 1;\n" +
     * "}\n" +
     * "LogSink(wordranks) {format:'json';}";
     * env.eventService().sendEvent(new CurrentTimeEvent(0));
     * env.compileDeploy(epl);
     * <p>
     * // prepare test
     * DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
     * EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
     * options.setOperatorProvider(new DefaultSupportGraphOpProvider(capture));
     * <p>
     * EPDataFlowInstance instanceOne = env.runtime().getDataFlowService().instantiate("RollingTopWords", options);
     * instanceOne.start();
     * <p>
     * Thread.sleep(100000000);
     * }
     */

    public static class MyWordTestSource implements DataFlowSourceOperator {

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        private int count;

        public void next() throws InterruptedException {
            Thread.sleep(100);
            final String[] words = new String[]{"this", "is", "a", "test"};
            final Random rand = new Random();
            final String word = words[rand.nextInt(words.length)];
            graphContext.submit(new Object[]{word});
        }

        public void open(DataFlowOpOpenContext openContext) {
        }

        public void close(DataFlowOpCloseContext openContext) {
        }
    }
}
