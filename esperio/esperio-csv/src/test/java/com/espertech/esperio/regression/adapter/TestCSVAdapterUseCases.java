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
package com.espertech.esperio.regression.adapter;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphParamProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esper.runtime.client.util.InputAdapter;
import com.espertech.esperio.csv.*;
import com.espertech.esperio.file.FileSourceCSV;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esperio.support.util.CompileUtil.compileDeploy;

public class TestCSVAdapterUseCases extends TestCase {
    private static String NEW_LINE = System.getProperty("line.separator");
    protected static String CSV_FILENAME_ONELINE_TRADE = "regression/csvtest_tradedata.csv";
    private static String CSV_FILENAME_ONELINE_TRADE_MULTIPLE = "regression/csvtest_tradedata_multiple.csv";
    private static String CSV_FILENAME_TIMESTAMPED_PRICES = "regression/csvtest_timestamp_prices.csv";
    private static String CSV_FILENAME_TIMESTAMPED_TRADES = "regression/csvtest_timestamp_trades.csv";

    protected EPRuntime runtime;
    private boolean useBean = false;

    public TestCSVAdapterUseCases() {
        this(false);
    }

    public TestCSVAdapterUseCases(boolean ub) {
        useBean = ub;
    }

    /**
     * Play a CSV file using an existing event type definition (no timestamps).
     * <p>
     * Should not require a timestamp column, should block thread until played in.
     */
    public void testExistingTypeNoOptions() {
        runtime = EPRuntimeProvider.getRuntime("testExistingTypeNoOptions", makeConfig("TypeA", useBean));
        runtime.initialize();

        EPStatement stmt = compileDeploy(runtime, "select symbol, price, volume from TypeA#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        (new CSVInputAdapter(runtime, new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "TypeA")).start();

        assertEquals(1, listener.getNewDataList().size());

        // test graph
        String graph = "create dataflow ReadCSV " +
            "FileSource -> mystream<TypeA> { file: '" + CSV_FILENAME_ONELINE_TRADE + "', hasTitleLine: true, classpathFile: true }" +
            "DefaultSupportCaptureOp(mystream) {}";
        EPDeployment deployment = compileDeploy(runtime, graph);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(deployment.getDeploymentId(), "ReadCSV", new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
        instance.run();
        List<List<Object>> received = outputOp.getAndReset();
        Object[] receivedArr = received.get(0).toArray();
        assertEquals(1, receivedArr.length);
    }

    /**
     * Play a CSV file that is from memory.
     */
    public void testPlayFromInputStream() throws Exception {
        String myCSV = "symbol, price, volume" + NEW_LINE + "IBM, 10.2, 10000";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(myCSV.getBytes());
        trySource(new AdapterInputSource(inputStream));
    }

    /**
     * Play a CSV file that is from memory.
     */
    public void testPlayFromStringReader() throws Exception {
        String myCSV = "symbol, price, volume" + NEW_LINE + "IBM, 10.2, 10000";
        StringReader reader = new StringReader(myCSV);
        trySource(new AdapterInputSource(reader));
    }

    /**
     * Play a CSV file using an runtime thread
     */
    public void testEngineThread1000PerSec() throws Exception {
        runtime = EPRuntimeProvider.getRuntime("testExistingTypeNoOptions", makeConfig("TypeA"));
        runtime.initialize();

        EPStatement stmt = compileDeploy(runtime, "select symbol, price, volume from TypeA#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        CSVInputAdapterSpec spec = new CSVInputAdapterSpec(new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "TypeA");
        spec.setEventsPerSec(1000);
        spec.setUsingRuntimeThread(true);

        InputAdapter inputAdapter = new CSVInputAdapter(runtime, spec);
        inputAdapter.start();
        Thread.sleep(1000);

        assertEquals(1, listener.getNewDataList().size());
    }

    /**
     * Play a CSV file using an runtime thread.
     */
    public void testEngineThread1PerSec() throws Exception {
        runtime = EPRuntimeProvider.getRuntime("testExistingTypeNoOptions", makeConfig("TypeA"));
        runtime.initialize();

        EPStatement stmt = compileDeploy(runtime, "select symbol, price, volume from TypeA#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        CSVInputAdapterSpec spec = new CSVInputAdapterSpec(new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE_MULTIPLE), "TypeA");
        spec.setEventsPerSec(1);
        spec.setUsingRuntimeThread(true);

        InputAdapter inputAdapter = new CSVInputAdapter(runtime, spec);
        inputAdapter.start();

        Thread.sleep(1500);
        assertEquals(1, listener.getNewDataList().size());
        listener.reset();
        Thread.sleep(300);
        assertEquals(0, listener.getNewDataList().size());

        Thread.sleep(2000);
        assertTrue(listener.getNewDataList().size() >= 2);
    }

    /**
     * Play a CSV file using the application thread
     */
    public void testAppThread() throws Exception {
        runtime = EPRuntimeProvider.getRuntime("testExistingTypeNoOptions", makeConfig("TypeA"));
        runtime.initialize();

        EPStatement stmt = compileDeploy(runtime, "select symbol, price, volume from TypeA#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        CSVInputAdapterSpec spec = new CSVInputAdapterSpec(new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "TypeA");
        spec.setEventsPerSec(1000);

        InputAdapter inputAdapter = new CSVInputAdapter(runtime, spec);
        inputAdapter.start();

        assertEquals(1, listener.getNewDataList().size());
    }

    /**
     * Play a CSV file using no existing (dynamic) event type (no timestamp)
     */
    public void testDynamicType() {
        CSVInputAdapterSpec spec = new CSVInputAdapterSpec(new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "TypeB");

        Configuration config = new Configuration();
        config.getRuntime().getThreading().setInternalTimerEnabled(false);
        runtime = EPRuntimeProvider.getDefaultRuntime(config);
        runtime.initialize();

        compileDeploy(runtime, "@public @buseventtype create schema TypeB(symbol string, price string, volume string)");

        InputAdapter feed = new CSVInputAdapter(runtime, spec);

        EPStatement stmt = compileDeploy(runtime, "select symbol, price, volume from TypeB#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(String.class, stmt.getEventType().getPropertyType("symbol"));
        assertEquals(String.class, stmt.getEventType().getPropertyType("price"));
        assertEquals(String.class, stmt.getEventType().getPropertyType("volume"));

        feed.start();
        assertEquals(1, listener.getNewDataList().size());
    }

    public void testCoordinated() throws Exception {
        Map<String, Object> priceProps = new HashMap<String, Object>();
        priceProps.put("timestamp", Long.class);
        priceProps.put("symbol", String.class);
        priceProps.put("price", Double.class);

        Map<String, Object> tradeProps = new HashMap<String, Object>();
        tradeProps.put("timestamp", Long.class);
        tradeProps.put("symbol", String.class);
        tradeProps.put("notional", Double.class);

        Configuration config = new Configuration();
        config.getCommon().addEventType("TradeEvent", tradeProps);
        config.getCommon().addEventType("PriceEvent", priceProps);

        runtime = EPRuntimeProvider.getRuntime("testCoordinated", config);
        runtime.initialize();
        runtime.getEventService().clockExternal();
        runtime.getEventService().advanceTime(0);

        AdapterInputSource sourcePrices = new AdapterInputSource(CSV_FILENAME_TIMESTAMPED_PRICES);
        CSVInputAdapterSpec inputPricesSpec = new CSVInputAdapterSpec(sourcePrices, "PriceEvent");
        inputPricesSpec.setTimestampColumn("timestamp");
        inputPricesSpec.setPropertyTypes(priceProps);
        CSVInputAdapter inputPrices = new CSVInputAdapter(inputPricesSpec);

        AdapterInputSource sourceTrades = new AdapterInputSource(CSV_FILENAME_TIMESTAMPED_TRADES);
        CSVInputAdapterSpec inputTradesSpec = new CSVInputAdapterSpec(sourceTrades, "TradeEvent");
        inputTradesSpec.setTimestampColumn("timestamp");
        inputTradesSpec.setPropertyTypes(tradeProps);
        CSVInputAdapter inputTrades = new CSVInputAdapter(inputTradesSpec);

        EPStatement stmtPrices = compileDeploy(runtime, "select symbol, price from PriceEvent#length(100)").getStatements()[0];
        SupportUpdateListener listenerPrice = new SupportUpdateListener();
        stmtPrices.addListener(listenerPrice);
        EPStatement stmtTrade = compileDeploy(runtime, "select symbol, notional from TradeEvent#length(100)").getStatements()[0];
        SupportUpdateListener listenerTrade = new SupportUpdateListener();
        stmtTrade.addListener(listenerTrade);

        AdapterCoordinator coordinator = new AdapterCoordinatorImpl(runtime, true);
        coordinator.coordinate(inputPrices);
        coordinator.coordinate(inputTrades);
        coordinator.start();

        runtime.getEventService().advanceTime(400);
        assertFalse(listenerTrade.isInvoked());
        assertFalse(listenerPrice.isInvoked());

        // invoke read of events at 500 (see CSV)
        runtime.getEventService().advanceTime(1000);
        assertEquals(1, listenerTrade.getNewDataList().size());
        assertEquals(1, listenerPrice.getNewDataList().size());
        listenerTrade.reset();
        listenerPrice.reset();

        // invoke read of price events at 1500 (see CSV)
        runtime.getEventService().advanceTime(2000);
        assertEquals(0, listenerTrade.getNewDataList().size());
        assertEquals(1, listenerPrice.getNewDataList().size());
        listenerTrade.reset();
        listenerPrice.reset();

        // invoke read of trade events at 2500 (see CSV)
        runtime.getEventService().advanceTime(3000);
        assertEquals(1, listenerTrade.getNewDataList().size());
        assertEquals(0, listenerPrice.getNewDataList().size());
        listenerTrade.reset();
        listenerPrice.reset();
    }

    private Configuration makeConfig(String typeName) {
        return makeConfig(typeName, false);
    }

    private Configuration makeConfig(String typeName, boolean useBean) {
        Configuration configuration = new Configuration();
        configuration.getCommon().addImport(FileSourceCSV.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");
        if (useBean) {
            configuration.getCommon().addEventType(typeName, ExampleMarketDataBean.class);
        } else {
            Map<String, Object> eventProperties = new HashMap<String, Object>();
            eventProperties.put("symbol", String.class);
            eventProperties.put("price", double.class);
            eventProperties.put("volume", Integer.class);
            configuration.getCommon().addEventType(typeName, eventProperties);
        }

        return configuration;
    }

    private void trySource(AdapterInputSource source) throws Exception {
        CSVInputAdapterSpec spec = new CSVInputAdapterSpec(source, "TypeC");

        runtime = EPRuntimeProvider.getRuntime("testPlayFromInputStream", makeConfig("TypeC"));
        runtime.initialize();
        InputAdapter feed = new CSVInputAdapter(runtime, spec);

        EPStatement stmt = compileDeploy(runtime, "select * from TypeC#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        feed.start();
        assertEquals(1, listener.getNewDataList().size());

        if (source.getAsReader() != null) {
            source.getAsReader().reset();
        } else {
            source.getAsStream().reset();
        }

        // test graph
        String graph = "create dataflow ReadCSV " +
            "FileSource -> mystream<TypeC> { hasTitleLine: true, classpathFile: true }" +
            "DefaultSupportCaptureOp(mystream) {}";
        EPDeployment deployment = compileDeploy(runtime, graph);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(outputOp));
        options.parameterProvider(new DefaultSupportGraphParamProvider(Collections.<String, Object>singletonMap("adapterInputSource", source)));
        EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(deployment.getDeploymentId(), "ReadCSV", options);
        instance.run();
        Object[] received = outputOp.getAndReset().get(0).toArray();
        assertEquals(1, received.length);
    }

    /**
     * Bean with same properties as map type used in this test
     */
    public static class ExampleMarketDataBean {
        private String symbol;
        private double price;
        private Integer volume;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public Integer getVolume() {
            return volume;
        }

        public void setVolume(Integer volume) {
            this.volume = volume;
        }
    }
}
