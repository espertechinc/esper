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
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOpForge;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import com.espertech.esperio.file.FileSourceForge;
import junit.framework.TestCase;

import java.util.List;

import static com.espertech.esperio.support.util.CompileUtil.compileDeploy;
import static com.espertech.esperio.support.util.CompileUtil.undeployAll;

public class TestFileSourceLineGraphs extends TestCase {
    private EPRuntime runtime;

    protected void setUp() {
        Configuration configuration = new Configuration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
        configuration.getCommon().addImport(FileSourceForge.class.getName());
        configuration.getCommon().addImport(DefaultSupportCaptureOpForge.class.getName());
        configuration.getCommon().addEventType("MyLineEvent", MyLineEvent.class);
        configuration.getCommon().addEventType("MyInvalidEvent", MyInvalidEvent.class);
        runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
        runtime.initialize();
    }

    public void testEndOfFileMarker() throws Exception {
        compileDeploy(runtime, "@public @buseventtype create objectarray schema MyBOF (filename string)");
        compileDeploy(runtime, "@public @buseventtype create objectarray schema MyEOF (filename string)");
        compileDeploy(runtime, "@public @buseventtype create objectarray schema MyLine (filename string, line string)");

        compileDeploy(runtime, "@public create context FileContext " +
            "initiated by MyBOF as mybof " +
            "terminated by MyEOF(filename=mybof.filename)");

        EPStatement stmtCount = compileDeploy(runtime, "context FileContext " +
            "select context.mybof.filename as filename, count(*) as cnt " +
            "from MyLine(filename=context.mybof.filename) " +
            "output snapshot when terminated").getStatements()[0];
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCount.addListener(listener);

        String epl = "create dataflow MyEOFEventFileReader " +
            "FileSource -> mylines<MyLine>, mybof<MyBOF>, myeof<MyEOF> { " +
            "classpathFile: true, numLoops: 1, format: 'line', " +
            "propertyNameLine: 'line', propertyNameFile: 'filename'}\n" +
            "EventBusSink(mylines, mybof, myeof) {}\n";
        EPDeployment deployment = compileDeploy(runtime, epl);

        for (String filename : new String[]{"regression/line_file_1.txt", "regression/line_file_2.txt"}) {
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            options.addParameterURI("FileSource/file", filename);
            EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(deployment.getDeploymentId(), "MyEOFEventFileReader", options);
            instance.run();
            assertEquals(1, instance.getParameters().size());
            assertEquals(filename, instance.getParameters().get("FileSource/file"));
        }

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getNewDataListFlattened(), "filename,cnt".split(","),
            new Object[][]{{"regression/line_file_1.txt", 3L}, {"regression/line_file_2.txt", 2L}});
    }

    public void testPropertyOrderWLoop() throws Exception {
        String graph = "create dataflow ReadCSV " +
            "create objectarray schema MyLine (line string)," +
            "FileSource -> mystream<MyLine> { file: 'regression/ints.csv', classpathFile: true, numLoops: 3, format: 'line'}" +
            "DefaultSupportCaptureOp(mystream) {}";
        List<List<Object>> received = runDataFlow(graph);
        assertEquals(1, received.size());
        Object[] compare = received.get(0).toArray();
        EPAssertionUtil.assertEqualsExactOrder(compare, new Object[]{new Object[]{"1, 0"}, new Object[]{"2, 0"}, new Object[]{"3, 0"}});
    }

    public void testZipFileLine() throws Exception {
        String graph = "create dataflow ReadCSV " +
            "create objectarray schema MyLine (line string)," +
            "FileSource -> mystream<MyLine> { file: 'regression/myzippedtext.zip', classpathFile: true, format: 'line'}" +
            "DefaultSupportCaptureOp(mystream) {}";
        List<List<Object>> received = runDataFlow(graph);
        assertEquals(1, received.size());
        Object[] compare = received.get(0).toArray();
        EPAssertionUtil.assertEqualsExactOrder(compare, new Object[]{new Object[]{"this is the first line"},
            new Object[]{"this is the second line"}, new Object[]{"this is the third line"}});
    }

    public void testFileBeanEvent() throws Exception {

        String graph = "create dataflow ReadCSV " +
            "FileSource -> mystream<MyLineEvent> { " +
            "  file: 'regression/myzippedtext.zip', " +
            "  classpathFile: true, " +
            "  format: 'line'," +
            "  propertyNameLine: 'theLine'" +
            "}" +
            "DefaultSupportCaptureOp(mystream) {}";
        List<List<Object>> received = runDataFlow(graph);
        assertEquals(1, received.size());
        Object[] compare = received.get(0).toArray();
        EPAssertionUtil.assertEqualsExactOrder(compare, new Object[]{new MyLineEvent("this is the first line"),
            new MyLineEvent("this is the second line"), new MyLineEvent("this is the third line")});
    }

    public void testInvalid() {
        String epl = "create dataflow FlowOne " +
            "FileSource -> mystream<MyInvalidEvent> { file: 'regression/myzippedtext.zip', classpathFile: true, format: 'line'," +
            "${SUBS_HERE}}" +
            "DefaultSupportCaptureOp(mystream) {}";

        tryInvalid("FlowOne", epl, "", "Failed to instantiate data flow 'FlowOne': Failed to obtain operator instance for 'FileSource': Expecting an output event type that has a single property that is of type string, or alternatively specify the 'propertyNameLine' parameter");

        tryInvalid("FlowOne", epl, "propertyNameLine: 'xxx'", "Failed to instantiate data flow 'FlowOne': Failed to obtain operator instance for 'FileSource': Failed to find property name 'xxx' in type 'MyInvalidEvent'");

        tryInvalid("FlowOne", epl, "propertyNameLine: 'someInt'", "Failed to instantiate data flow 'FlowOne': Failed to obtain operator instance for 'FileSource': Invalid property type for property 'someInt', expected a property of type String");
    }

    private void tryInvalid(String dataflowName, String epl, String substituion, String message) {
        epl = epl.replace("${SUBS_HERE}", substituion);
        EPStatement stmtGraph = compileDeploy(runtime, epl).getStatements()[0];
        try {
            DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
            runtime.getDataFlowService().instantiate(stmtGraph.getDeploymentId(), dataflowName,
                new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        } finally {
            undeployAll(runtime);
        }
    }

    private List<List<Object>> runDataFlow(String epl) {
        EPDeployment deployment = compileDeploy(runtime, epl);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstance instance = runtime.getDataFlowService().instantiate(deployment.getDeploymentId(), "ReadCSV",
            new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
        instance.run();
        return outputOp.getAndReset();
    }

    public static final class MyLineEvent {
        private String theLine;

        public MyLineEvent() {
        }

        public MyLineEvent(String theLine) {
            this.theLine = theLine;
        }

        public String getTheLine() {
            return theLine;
        }

        public void setTheLine(String theLine) {
            this.theLine = theLine;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MyLineEvent that = (MyLineEvent) o;

            if (theLine != null ? !theLine.equals(that.theLine) : that.theLine != null) return false;

            return true;
        }

        public int hashCode() {
            return theLine != null ? theLine.hashCode() : 0;
        }
    }

    public static class MyInvalidEvent {
        private int someInt;

        public int getSomeInt() {
            return someInt;
        }

        public void setSomeInt(int someInt) {
            this.someInt = someInt;
        }
    }
}
