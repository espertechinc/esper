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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportGraphParamProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.dataflow.SupportDataFlowAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecDataflowOpEPStatementSource implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionStmtNameDynamic(epService);
        runAssertionAllTypes(epService);
        runAssertionInvalid(epService);
        runAssertionStatementFilter(epService);
    }

    private void runAssertionStmtNameDynamic(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "create map schema SingleProp (id string), " +
                "EPStatementSource -> thedata<SingleProp> {" +
                "  statementName : 'MyStatement'," +
                "} " +
                "DefaultSupportCaptureOp(thedata) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        assertNull(df.getUserObject());
        assertNull(df.getInstanceId());
        df.start();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(0, captureOp.getCurrent().length);

        EPStatement stmt = epService.getEPAdministrator().createEPL("@Name('MyStatement') select theString as id from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        captureOp.waitForInvocation(100, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"E2"});

        stmt.stop();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertEquals(0, captureOp.getCurrent().length);

        stmt.start();

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        captureOp.waitForInvocation(100, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"E4"});

        stmt.destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        assertEquals(0, captureOp.getCurrent().length);

        epService.getEPAdministrator().createEPL("@Name('MyStatement') select 'X'||theString||'X' as id from SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));
        captureOp.waitForInvocation(100, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"XE6X"});

        df.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAllTypes(EPServiceProvider epService) throws Exception {
        DefaultSupportGraphEventUtil.addTypeConfiguration(epService);

        runAssertionStatementNameExists(epService, "MyMapEvent", DefaultSupportGraphEventUtil.getMapEvents());
        runAssertionStatementNameExists(epService, "MyOAEvent", DefaultSupportGraphEventUtil.getOAEvents());
        runAssertionStatementNameExists(epService, "MyEvent", DefaultSupportGraphEventUtil.getPOJOEvents());
        runAssertionStatementNameExists(epService, "MyXMLEvent", DefaultSupportGraphEventUtil.getXMLEvents());

        // test doc samples
        String epl = "create dataflow MyDataFlow\n" +
                "  create schema SampleSchema(tagId string, locX double),\t// sample type\t\t\t\n" +
                "\t\t\t\n" +
                "  // Consider only the statement named MySelectStatement when it exists.\n" +
                "  EPStatementSource -> stream.one<eventbean<?>> {\n" +
                "    statementName : 'MySelectStatement'\n" +
                "  }\n" +
                "  \n" +
                "  // Consider all statements that match the filter object provided.\n" +
                "  EPStatementSource -> stream.two<eventbean<?>> {\n" +
                "    statementFilter : {\n" +
                "      class : '" + MyFilter.class.getName() + "'\n" +
                "    }\n" +
                "  }\n" +
                "  \n" +
                "  // Consider all statements that match the filter object provided.\n" +
                "  // With collector that performs transformation.\n" +
                "  EPStatementSource -> stream.two<SampleSchema> {\n" +
                "    collector : {\n" +
                "      class : '" + MyCollector.class.getName() + "'\n" +
                "    },\n" +
                "    statementFilter : {\n" +
                "      class : '" + MyFilter.class.getName() + "'\n" +
                "    }\n" +
                "  }";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        // test no statement name or statement filter provided
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 EPStatementSource -> abc {}",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'EPStatementSource': Failed to find required 'statementName' or 'statementFilter' parameter");

        // invalid: no output stream
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "DF1", "create dataflow DF1 EPStatementSource { statementName : 'abc' }",
                "Failed to instantiate data flow 'DF1': Failed initialization for operator 'EPStatementSource': EPStatementSource operator requires one output stream but produces 0 streams");
    }

    private void runAssertionStatementFilter(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_B.class);

        // one statement exists before the data flow
        EPStatement stmt = epService.getEPAdministrator().createEPL("select id from SupportBean_B");

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "create schema AllObjects Object," +
                "EPStatementSource -> thedata<AllObjects> {} " +
                "DefaultSupportCaptureOp(thedata) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>();
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        MyFilter myFilter = new MyFilter();
        options.parameterProvider(new DefaultSupportGraphParamProvider(Collections.<String, Object>singletonMap("statementFilter", myFilter)));
        options.operatorProvider(new DefaultSupportGraphOpProvider(captureOp));
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        captureOp.waitForInvocation(200, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"B1"});

        epService.getEPAdministrator().createEPL("select theString, intPrimitive from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        captureOp.waitForInvocation(200, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "theString,intPrimitive".split(","), new Object[]{"E1", 1});

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select id from SupportBean_A");
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        captureOp.waitForInvocation(200, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"A1"});

        stmtTwo.stop();

        epService.getEPRuntime().sendEvent(new SupportBean_A("A2"));
        Thread.sleep(50);
        assertEquals(0, captureOp.getCurrent().length);

        stmtTwo.start();

        epService.getEPRuntime().sendEvent(new SupportBean_A("A3"));
        captureOp.waitForInvocation(200, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"A3"});

        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        captureOp.waitForInvocation(200, 1);
        EPAssertionUtil.assertProps(captureOp.getCurrentAndReset()[0], "id".split(","), new Object[]{"B2"});

        df.cancel();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B3"));
        assertEquals(0, captureOp.getCurrent().length);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionStatementNameExists(EPServiceProvider epService, String typeName, Object[] events) throws Exception {
        epService.getEPAdministrator().createEPL("@Name('MyStatement') select * from " + typeName);

        epService.getEPAdministrator().createEPL("create dataflow MyDataFlowOne " +
                "create schema AllObject java.lang.Object," +
                "EPStatementSource -> thedata<AllObject> {" +
                "  statementName : 'MyStatement'," +
                "} " +
                "DefaultSupportCaptureOp(thedata) {}");

        DefaultSupportCaptureOp<Object> captureOp = new DefaultSupportCaptureOp<Object>(2);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(captureOp));

        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlowOne", options);
        df.start();

        EventSender sender = epService.getEPRuntime().getEventSender(typeName);
        for (Object event : events) {
            sender.sendEvent(event);
        }

        captureOp.get(1, TimeUnit.SECONDS);
        EPAssertionUtil.assertEqualsExactOrder(events, captureOp.getCurrent());

        df.cancel();
        epService.getEPAdministrator().destroyAllStatements();
    }

    public static class MyFilter implements EPDataFlowEPStatementFilter {
        public boolean pass(EPStatement statement) {
            return true;
        }
    }

    public static class MyCollector implements EPDataFlowIRStreamCollector {
        public void collect(EPDataFlowIRStreamCollectorContext data) {
        }
    }
}
