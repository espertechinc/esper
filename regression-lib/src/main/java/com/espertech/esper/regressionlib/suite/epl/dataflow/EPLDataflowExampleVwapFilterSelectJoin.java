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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementFormatter;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.dataflow.MyObjectArrayGraphSource;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class EPLDataflowExampleVwapFilterSelectJoin implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        if (env.isHA()) {
            return;
        }

        String epl = "@name('flow')create dataflow VWAPSample\r\n" +
            "create objectarray schema TradeQuoteType as (type string, ticker string, price double, volume long, askprice double, asksize long),\n" +
            "MyObjectArrayGraphSource -> TradeQuoteStream<TradeQuoteType> {}\r\n" +
            "Filter(TradeQuoteStream) -> TradeStream {\r\n" +
            "filter: type=\"trade\"\r\n" +
            "}\r\n" +
            "Filter(TradeQuoteStream) -> QuoteStream {\r\n" +
            "filter: type=\"quote\"\r\n" +
            "}\r\n" +
            "Select(TradeStream) -> VwapTrades {\r\n" +
            "select: (select ticker, sum(price*volume)/sum(volume) as vwap, min(price) as minprice from TradeStream#groupwin(ticker)#length(4) group by ticker)\r\n" +
            "}\r\n" +
            "Select(VwapTrades as T, QuoteStream as Q) -> BargainIndex {\r\n" +
            "select: " +
            "(select case when vwap>askprice then asksize*(Math.exp(vwap-askprice)) else 0.0d end as index " +
            "from T#unique(ticker) as t, Q#lastevent as q " +
            "where t.ticker=q.ticker)\r\n" +
            "}\r\n" +
            "DefaultSupportCaptureOp(BargainIndex) {}\r\n";
        env.compileDeploy(epl);

        runAssertion(env);

        env.undeployAll();
        EPStatementObjectModel model = env.eplToModel(epl);
        String text = model.toEPL(new EPStatementFormatter(true));
        assertEquals(removeNewlines(epl), removeNewlines(text));
        env.compileDeploy(model);

        runAssertion(env);
        env.undeployAll();
    }

    private static void runAssertion(RegressionEnvironment env) {

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        MyObjectArrayGraphSource source = new MyObjectArrayGraphSource(Arrays.asList(
            new Object[]{"trade", "GE", 100d, 1000L, null, null}, // vwap = 100, minPrice=100
            new Object[]{"quote", "GE", null, null, 99.5d, 2000L}  //
        ).iterator());

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
            .operatorProvider(new DefaultSupportGraphOpProvider(future, source));

        env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "VWAPSample", options).start();

        Object[] received;
        try {
            received = future.get(5, TimeUnit.SECONDS);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        assertEquals(1, received.length);
        EPAssertionUtil.assertProps(received[0], "index".split(","), new Object[]{2000 * Math.exp(100 - 99.5)});

        env.undeployAll();
    }

    private String removeNewlines(String text) {
        return text.replace("\n", "").replace("\r", "");
    }
}
