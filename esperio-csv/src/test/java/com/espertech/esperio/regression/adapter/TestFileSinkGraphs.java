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
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationException;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.util.FileUtil;
import com.espertech.esperio.file.FileSinkFactory;
import junit.framework.TestCase;

public class TestFileSinkGraphs extends TestCase {
    private EPServiceProvider epService;

    protected void setUp() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.addImport(FileSinkFactory.class.getPackage().getName() + ".*");
        configuration.addImport(DefaultSupportSourceOp.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);
    }

    public void testInvalid() {
        String graph;

        graph = "create dataflow FlowOne " +
                "DefaultSupportSourceOp -> mystreamOne<MyMapEvent> {}" +
                "FileSink(mystreamOne, mystreamOne) {}";
        tryInvalidInstantiate("FlowOne", graph, "Failed to instantiate data flow 'FlowOne': Failed to obtain operator 'FileSink', encountered an exception raised by factory class FileSinkFactory: Failed to find required 'file' parameter");

        graph = "create dataflow FlowOne " +
                "DefaultSupportSourceOp -> mystreamOne<MyMapEvent> {}" +
                "FileSink(mystreamOne, mystreamOne) {file: 'x:\\a.bb'}";
        tryInvalidInstantiate("FlowOne", graph, "Failed to instantiate data flow 'FlowOne': Failed initialization for operator 'FileSink': FileSinkCSV expected a single input port");
    }

    private void tryInvalidInstantiate(String dataflowName, String epl, String message) {
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(epl);
        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        try {
            epService.getEPRuntime().getDataFlowRuntime().instantiate(dataflowName,
                    new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        }
        stmtGraph.destroy();
    }

    public void testWriteCSV() throws Exception {
        runAssertion("MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents(), true);
        runAssertion("MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents(), true);
        runAssertion("MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents(), false);
        runAssertion("MyEvent", DefaultSupportGraphEventUtil.getPOJOEvents(), true);
    }

    private void runAssertion(String typeName, Object[] events, boolean append) throws Exception {
        // test classpath file
        String filename = "regression/out_1.csv";
        FileUtil.findDeleteClasspathFile(filename);

        String graph = "create dataflow WriteCSV " +
                "DefaultSupportSourceOp -> instream<" + typeName + ">{}" +
                "FileSink(instream) { file: '" + filename + "', classpathFile: true, append: " + append + "}";
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(graph);

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("WriteCSV", options);
        instance.run();

        String[] contents = FileUtil.readClasspathTextFile(filename);
        EPAssertionUtil.assertEqualsExactOrder("1.1,1,\"one\";2.2,2,\"two\"".split(";"), contents);
        FileUtil.findDeleteClasspathFile(filename);

        stmtGraph.destroy();
    }
}
