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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.dataflow.ops.Emitter;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ExecDataflowExampleRollingTopWords implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(this.getClass().getName());

        String epl = "create dataflow RollingTopWords\n" +
                "create objectarray schema WordEvent (word string),\n" +
                "Emitter -> wordstream<WordEvent> {name:'a'} // Produces word stream\n" +
                "Select(wordstream) -> wordcount { // Sliding time window count per word\n" +
                "  select: (select word, count(*) as wordcount from wordstream#time(30) group by word)\n" +
                "}\n" +
                "Select(wordcount) -> wordranks { // Rank of words\n" +
                "  select: (select window(*) as rankedWords from wordcount#sort(3, wordcount desc) output snapshot every 2 seconds)\n" +
                "}\n" +
                "DefaultSupportCaptureOp(wordranks) {}";
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        epService.getEPAdministrator().createEPL(epl);

        // prepare test
        DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(capture));

        EPDataFlowInstance instanceOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("RollingTopWords", options);
        Emitter emitter = instanceOne.startCaptive().getEmitters().get("a");

        for (String word : new String[]{"this", "is", "a", "test", "that", "is", "a", "word", "test"}) {
            emitter.submit(new Object[]{word});
        }
        assertEquals(0, capture.getCurrentAndReset().length);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(2000));
        assertEquals(1, capture.getCurrent().length);
        Map map = (Map) capture.getCurrent()[0];
        Object[][] rows = (Object[][]) map.get("rankedWords");
        EPAssertionUtil.assertPropsPerRow(rows, "word,count".split(","), new Object[][]{{"is", 2L}, {"a", 2L}, {"test", 2L}});

        instanceOne.cancel();
    }

    /**
     * Comment-In for online flow-testing.
     * <p>
     * public void testOnline() throws Exception {
     * epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
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
     * epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
     * epService.getEPAdministrator().createEPL(epl);
     * <p>
     * // prepare test
     * DefaultSupportCaptureOp capture = new DefaultSupportCaptureOp();
     * EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
     * options.setOperatorProvider(new DefaultSupportGraphOpProvider(capture));
     * <p>
     * EPDataFlowInstance instanceOne = epService.getEPRuntime().getDataFlowRuntime().instantiate("RollingTopWords", options);
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

        public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
            return null;
        }

        public void open(DataFlowOpOpenContext openContext) {
        }

        public void close(DataFlowOpCloseContext openContext) {
        }
    }
}
