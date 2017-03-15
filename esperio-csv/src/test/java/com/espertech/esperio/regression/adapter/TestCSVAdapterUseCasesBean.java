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

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.file.FileSourceCSV;
import com.espertech.esperio.support.util.ExampleMarketDataBeanReadWrite;

/**
 * Cause all parent class unit tests to be run but sending beans instead of Maps
 *
 * @author Jerry Shea
 */
public class TestCSVAdapterUseCasesBean extends TestCSVAdapterUseCases {

    public TestCSVAdapterUseCasesBean() {
        super(true);
    }

    public void testReadWritePropsBean() {
        Configuration configuration = new Configuration();
        configuration.addEventType("ReadWrite", ExampleMarketDataBeanReadWrite.class);
        configuration.addImport(FileSourceCSV.class.getPackage().getName() + ".*");

        epService = EPServiceProviderManager.getProvider("testExistingTypeNoOptions", configuration);
        epService.initialize();

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from ReadWrite#length(100)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        (new CSVInputAdapter(epService, new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "ReadWrite")).start();

        assertEquals(1, listener.getNewDataList().size());
        EventBean eb = listener.getNewDataList().get(0)[0];
        assertTrue(ExampleMarketDataBeanReadWrite.class == eb.getUnderlying().getClass());
        assertEquals(55.5 * 1000, eb.get("value"));

        // test graph
        String graph = "create dataflow ReadCSV " +
                "FileSource -> mystream<ReadWrite> { file: '" + CSV_FILENAME_ONELINE_TRADE + "', hasTitleLine: true, classpathFile: true }" +
                "DefaultSupportCaptureOp(mystream) {}";
        epService.getEPAdministrator().createEPL(graph);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("ReadCSV", new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
        instance.run();
        Object[] received = outputOp.getAndReset().get(0).toArray();
        assertEquals(1, received.length);
        assertEquals(55.5 * 1000, ((ExampleMarketDataBeanReadWrite) received[0]).getValue());
    }
}
