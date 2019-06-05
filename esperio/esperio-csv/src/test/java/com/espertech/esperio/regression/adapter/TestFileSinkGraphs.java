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
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationException;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.*;
import com.espertech.esper.common.internal.util.FileUtil;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.EPUndeployException;
import com.espertech.esperio.file.FileSinkFactory;
import junit.framework.TestCase;

import static com.espertech.esperio.support.util.CompileUtil.*;

public class TestFileSinkGraphs extends TestCase {
    private EPRuntime runtime;

    protected void setUp() {
        Configuration configuration = new Configuration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addImport(FileSinkFactory.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(DefaultSupportSourceOpForge.class.getName());
        DefaultSupportGraphEventUtil.addTypeConfiguration(configuration);
        runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
        runtime.initialize();
    }

    public void testInvalid() {
        String graph;

        graph = "create dataflow FlowOne " +
            "DefaultSupportSourceOp -> mystreamOne<MyMapEvent> {}" +
            "FileSink(mystreamOne, mystreamOne) {file: 'x:\\a.bb'}";
        tryInvalidCompileGraph(runtime, graph, "Failed to obtain operator 'FileSink': FileSinkForge expected a single input port");

        graph = "create dataflow FlowOne " +
            "DefaultSupportSourceOp -> mystreamOne<MyMapEvent> {}" +
            "FileSink(mystreamOne) {}";
        tryInvalidInstantiate("FlowOne", graph, "Failed to instantiate data flow 'FlowOne': Failed to obtain operator instance for 'FileSink': Parameter by name 'file' has no value");
    }

    private void tryInvalidInstantiate(String dataflowName, String epl, String message) {
        EPStatement stmtGraph = compileDeploy(runtime, epl).getStatements()[0];
        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        try {
            runtime.getDataFlowService().instantiate(stmtGraph.getDeploymentId(), dataflowName,
                new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        }

        try {
            runtime.getDeploymentService().undeployAll();
        } catch (EPUndeployException e) {
            throw new RuntimeException(e);
        }
    }

    public void testWriteCSV() throws Exception {
        runAssertion("MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents(), true);
        runAssertion("MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents(), true);
        runAssertion("MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents(), false);
        runAssertion("MyDefaultSupportGraphEvent", DefaultSupportGraphEventUtil.getPOJOEvents(), true);

        compileDeploy(runtime, "@public @buseventtype create json schema MyJsonEvent(myDouble double, myInt int, myString string)");
        runAssertion("MyJsonEvent", DefaultSupportGraphEventUtil.getJsonEvents(), true);
    }

    private void runAssertion(String typeName, Object[] events, boolean append) throws Exception {
        // test classpath file
        String filename = "regression/out_1.csv";
        FileUtil.findDeleteClasspathFile(filename);

        String graph = "create dataflow WriteCSV " +
            "DefaultSupportSourceOp -> instream<" + typeName + ">{}" +
            "FileSink(instream) { file: '" + filename + "', classpathFile: true, append: " + append + "}";
        EPStatement stmtGraph = compileDeploy(runtime, graph).getStatements()[0];

        DefaultSupportSourceOp source = new DefaultSupportSourceOp(events);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(source));
        EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(stmtGraph.getDeploymentId(), "WriteCSV", options);
        instance.run();

        String[] contents = FileUtil.readClasspathTextFile(filename);
        EPAssertionUtil.assertEqualsExactOrder("1.1,1,\"one\";2.2,2,\"two\"".split(";"), contents);
        FileUtil.findDeleteClasspathFile(filename);

        undeployAll(runtime);
    }
}
