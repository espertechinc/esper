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
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.supportregression.dataflow.MyLineFeedSource;
import com.espertech.esper.supportregression.dataflow.MyTokenizerCounter;
import com.espertech.esper.supportregression.dataflow.MyWordCountStats;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ExecDataflowExampleWordCount implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(MyTokenizerCounter.class.getPackage().getName() + ".*");
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");

        String epl = "create dataflow WordCount " +
                "MyLineFeedSource -> LineOfTextStream {} " +
                "MyTokenizerCounter(LineOfTextStream) -> SingleLineCountStream {}" +
                "MyWordCountAggregator(SingleLineCountStream) -> WordCountStream {}" +
                "DefaultSupportCaptureOp(WordCountStream) {}";
        epService.getEPAdministrator().createEPL(epl);

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        MyLineFeedSource source = new MyLineFeedSource(Arrays.asList("Test this code", "Test line two").iterator());

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future, source));

        epService.getEPRuntime().getDataFlowRuntime().instantiate("WordCount", options).start();

        Object[] received = future.get(3, TimeUnit.SECONDS);
        assertEquals(1, received.length);
        MyWordCountStats stats = (MyWordCountStats) received[0];
        EPAssertionUtil.assertProps(stats, "lines,words,chars".split(","), new Object[]{2, 6, 23});
    }
}
