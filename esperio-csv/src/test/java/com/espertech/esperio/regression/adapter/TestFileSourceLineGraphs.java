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
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esperio.file.FileSourceFactory;
import junit.framework.TestCase;

import java.util.List;

public class TestFileSourceLineGraphs extends TestCase {
    private EPServiceProvider epService;

    protected void setUp() {
        Configuration configuration = new Configuration();
        configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        configuration.addImport(FileSourceFactory.class.getName());
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
    }

    public void testEndOfFileMarker() throws Exception {
        epService.getEPAdministrator().createEPL("create objectarray schema MyBOF (filename string)");
        epService.getEPAdministrator().createEPL("create objectarray schema MyEOF (filename string)");
        epService.getEPAdministrator().createEPL("create objectarray schema MyLine (filename string, line string)");

        epService.getEPAdministrator().createEPL("create context FileContext " +
                "initiated by MyBOF as mybof " +
                "terminated by MyEOF(filename=mybof.filename)");

        EPStatement stmtCount = epService.getEPAdministrator().createEPL("context FileContext " +
                "select context.mybof.filename as filename, count(*) as cnt " +
                "from MyLine(filename=context.mybof.filename) " +
                "output snapshot when terminated");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtCount.addListener(listener);

        String epl = "create dataflow MyEOFEventFileReader " +
                "FileSource -> mylines<MyLine>, mybof<MyBOF>, myeof<MyEOF> { " +
                "classpathFile: true, numLoops: 1, format: 'line', " +
                "propertyNameLine: 'line', propertyNameFile: 'filename'}\n" +
                "EventBusSink(mylines, mybof, myeof) {}\n";
        epService.getEPAdministrator().createEPL(epl);

        for (String filename : new String[]{"regression/line_file_1.txt", "regression/line_file_2.txt"}) {
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            options.addParameterURI("FileSource/file", filename);
            EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyEOFEventFileReader", options);
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
        epService.getEPAdministrator().getConfiguration().addEventType("MyLineEvent", MyLineEvent.class);

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
        epService.getEPAdministrator().getConfiguration().addEventType("MyLineEvent", MyLineEvent.class);

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
        String graph;
        epService.getEPAdministrator().getConfiguration().addEventType("MyInvalidEvent", MyInvalidEvent.class);

        String epl = "create dataflow FlowOne " +
                "FileSource -> mystream<MyInvalidEvent> { file: 'regression/myzippedtext.zip', classpathFile: true, format: 'line'," +
                "${SUBS_HERE}}" +
                "DefaultSupportCaptureOp(mystream) {}";

        tryInvalid("FlowOne", epl, "", "Failed to instantiate data flow 'FlowOne': Failed initialization for operator 'FileSource': Expecting an output event type that has a single property that is of type string, or alternatively specify the 'propertyNameLine' parameter");

        tryInvalid("FlowOne", epl, "propertyNameLine: 'xxx'", "Failed to instantiate data flow 'FlowOne': Failed initialization for operator 'FileSource': Failed to find property name 'xxx' in type 'MyInvalidEvent'");

        tryInvalid("FlowOne", epl, "propertyNameLine: 'someInt'", "Failed to instantiate data flow 'FlowOne': Failed initialization for operator 'FileSource': Invalid property type for property 'someInt', expected a property of type String");
    }

    private void tryInvalid(String dataflowName, String epl, String substituion, String message) {
        epl = epl.replace("${SUBS_HERE}", substituion);
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL(epl);
        try {
            DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
            epService.getEPRuntime().getDataFlowRuntime().instantiate(dataflowName,
                    new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(outputOp)));
            fail();
        } catch (EPDataFlowInstantiationException ex) {
            assertEquals(message, ex.getMessage());
        } finally {
            stmtGraph.destroy();
        }
    }

    private List<List<Object>> runDataFlow(String epl) {
        epService.getEPAdministrator().createEPL(epl);

        DefaultSupportCaptureOp<Object> outputOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstance instance = epService.getEPRuntime().getDataFlowRuntime().instantiate("ReadCSV",
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
