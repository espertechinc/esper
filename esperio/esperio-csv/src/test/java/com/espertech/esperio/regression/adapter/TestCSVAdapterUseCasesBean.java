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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esperio.csv.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.file.FileSourceCSV;
import com.espertech.esperio.support.util.ExampleMarketDataBeanReadWrite;

import static com.espertech.esperio.support.util.CompileUtil.compileDeploy;

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
        configuration.getCommon().addEventType("ExampleMarketDataBeanReadWrite", ExampleMarketDataBeanReadWrite.class);
        configuration.getCommon().addImport(FileSourceCSV.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");

        runtime = EPRuntimeProvider.getRuntime("testExistingTypeNoOptions", configuration);
        runtime.initialize();

        EPStatement stmt = compileDeploy(runtime, "select * from ExampleMarketDataBeanReadWrite#length(100)").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        (new CSVInputAdapter(runtime, new AdapterInputSource(CSV_FILENAME_ONELINE_TRADE), "ExampleMarketDataBeanReadWrite")).start();

        assertEquals(1, listener.getNewDataList().size());
        EventBean eb = listener.getNewDataList().get(0)[0];
        assertTrue(ExampleMarketDataBeanReadWrite.class == eb.getUnderlying().getClass());
        assertEquals(55.5 * 1000, eb.get("value"));

        // test graph
        String graph = "create dataflow ReadCSV " +
            "FileSource -> mystream<ExampleMarketDataBeanReadWrite> { file: '" + CSV_FILENAME_ONELINE_TRADE + "', hasTitleLine: true, classpathFile: true }" +
            "DefaultSupportCaptureOp(mystream) {}";
        EPDeployment deployment = compileDeploy(runtime, graph);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(deployment.getDeploymentId(), "ReadCSV", new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
        instance.run();
        Object[] received = outputOp.getAndReset().get(0).toArray();
        assertEquals(1, received.length);
        assertEquals(55.5 * 1000, ((ExampleMarketDataBeanReadWrite) received[0]).getValue());
    }
}
