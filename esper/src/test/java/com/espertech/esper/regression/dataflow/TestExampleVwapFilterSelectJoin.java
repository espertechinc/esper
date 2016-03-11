/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.soda.EPStatementFormatter;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestExampleVwapFilterSelectJoin extends TestCase {

    private EPServiceProvider epService;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");
        epService.getEPAdministrator().getConfiguration().addImport(MyObjectArrayGraphSource.class.getPackage().getName() + ".*");
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testEPLGraphOnly() throws Exception {

        String epl = "create dataflow VWAPSample\r\n" +
                "create objectarray schema TradeQuoteType as (type string, ticker string, price double, volume long, askprice double, asksize long),\n" +
                "MyObjectArrayGraphSource -> TradeQuoteStream<TradeQuoteType> {}\r\n" +
                "Filter(TradeQuoteStream) -> TradeStream {\r\n" +
                "filter: type=\"trade\"\r\n" +
                "}\r\n" +
                "Filter(TradeQuoteStream) -> QuoteStream {\r\n" +
                "filter: type=\"quote\"\r\n" +
                "}\r\n" +
                "Select(TradeStream) -> VwapTrades {\r\n" +
                "select: (select ticker, sum(price*volume)/sum(volume) as vwap, min(price) as minprice from TradeStream.std:groupwin(ticker).win:length(4) group by ticker)\r\n" +
                "}\r\n" +
                "Select(VwapTrades as T, QuoteStream as Q) -> BargainIndex {\r\n" +
                "select: " +
                "(select case when vwap>askprice then asksize*(Math.exp(vwap-askprice)) else 0.0d end as index " +
                "from T.std:unique(ticker) as t, Q.std:lastevent() as q " +
                "where t.ticker=q.ticker)\r\n" +
                "}\r\n" +
                "DefaultSupportCaptureOp(BargainIndex) {}\r\n";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(epl);

        runAssertion();

        stmtGraph.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        String text = model.toEPL(new EPStatementFormatter(true));
        assertEquals(removeNewlines(epl), removeNewlines(text));
        epService.getEPAdministrator().create(model);

        runAssertion();
    }

    private void runAssertion() throws Exception {

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        MyObjectArrayGraphSource source = new MyObjectArrayGraphSource(Arrays.asList(
                new Object[] {"trade", "GE", 100d, 1000L, null, null}, // vwap = 100, minPrice=100
                new Object[] {"quote", "GE", null, null, 99.5d, 2000L}  //
                ).iterator());

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future, source));

        epService.getEPRuntime().getDataFlowRuntime().instantiate("VWAPSample", options).start();

        Object[] received = future.get(5, TimeUnit.SECONDS);
        assertEquals(1, received.length);
        EPAssertionUtil.assertProps(received[0], "index".split(","), new Object[] {2000*Math.exp(100-99.5)});
    }

    private String removeNewlines(String text) {
        return text.replace("\n", "").replace("\r", "");
    }
}
