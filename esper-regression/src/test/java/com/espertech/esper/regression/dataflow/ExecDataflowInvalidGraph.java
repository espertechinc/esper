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
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.dataflow.SupportDataFlowAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecDataflowInvalidGraph implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalidSyntax(epService);
        runAssertionInvalidGraph(epService);
    }

    private void runAssertionInvalidSyntax(EPServiceProvider epService) throws Exception {
        SupportDataFlowAssertionUtil.tryInvalidCreate(epService, "create dataflow MyGraph MySource -> select",
                "Incorrect syntax near 'select' (a reserved keyword) at line 1 column 36 [");

        SupportDataFlowAssertionUtil.tryInvalidCreate(epService, "create dataflow MyGraph MySource -> myout",
                "Incorrect syntax near end-of-input expecting a left curly bracket '{' but found end-of-input at line 1 column 41 [");
    }

    private void runAssertionInvalidGraph(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportSourceOp.class);
        epService.getEPAdministrator().getConfiguration().addImport(DefaultSupportCaptureOp.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean_B.class);
        epService.getEPAdministrator().getConfiguration().addImport(MyInvalidOpFactory.class);
        epService.getEPAdministrator().getConfiguration().addImport(MyTestOp.class);
        epService.getEPAdministrator().getConfiguration().addImport(MySBInputOp.class);
        String epl;

        // type not found
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph DefaultSupportSourceOp -> outstream<ABC> {}",
                "Failed to instantiate data flow 'MyGraph': Failed to find event type 'ABC'");

        // invalid schema (need not test all variants, same as create-schema)
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph create schema DUMMY com.mycompany.DUMMY, " +
                        "DefaultSupportSourceOp -> outstream<?> {}",
                "Failed to instantiate data flow 'MyGraph': Failed to resolve class 'com.mycompany.DUMMY': Could not load class by name 'com.mycompany.DUMMY', please check imports");

        // can't find op
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph DummyOp {}",
                "Failed to instantiate data flow 'MyGraph': Failed to resolve operator 'DummyOp': Could not load class by name 'DummyOp', please check imports");

        // op is some other class
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph Random {}",
                "Failed to instantiate data flow 'MyGraph': Failed to resolve operator 'Random', operator class java.util.Random does not declare the DataFlowOperator annotation or implement the DataFlowSourceOperator interface");

        // input stream not found
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph DefaultSupportCaptureOp(nostream) {}",
                "Failed to instantiate data flow 'MyGraph': Input stream 'nostream' consumed by operator 'DefaultSupportCaptureOp' could not be found");

        // failed op factory
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph MyInvalidOp {}",
                "Failed to instantiate data flow 'MyGraph': Failed to obtain operator 'MyInvalidOp', encountered an exception raised by factory class MyInvalidOpFactory: Failed-Here");

        // inject properties: property not found
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph DefaultSupportCaptureOp {dummy: 1}",
                "Failed to instantiate data flow 'MyGraph': Failed to find writable property 'dummy' for class");

        // inject properties: property invalid type
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", "create dataflow MyGraph MyTestOp {theString: 1}",
                "Failed to instantiate data flow 'MyGraph': Property 'theString' of class com.espertech.esper.regression.dataflow.ExecDataflowInvalidGraph$MyTestOp expects an java.lang.String but receives a value of type java.lang.Integer");

        // two incompatible input streams: different types
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'MyTestOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out2' typed 'SupportBean_B'");

        // two incompatible input streams: one is wildcard
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<?> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'MyTestOp' streams 'out1' and 'out2' have differing wildcard type information");

        // two incompatible input streams: underlying versus eventbean
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<Eventbean<SupportBean_B>> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'MyTestOp' streams 'out1' and 'out2' have differing underlying information");

        // output stream multiple type parameters
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A, SupportBean_B> {}";
        SupportDataFlowAssertionUtil.tryInvalidCreate(epService, epl,
                "Error starting statement: Failed to validate operator 'DefaultSupportSourceOp': Multiple output types for a single stream 'out1' are not supported [");

        // same output stream declared twice
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A>, out1<SupportBean_B> {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'DefaultSupportSourceOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<Eventbean<SupportBean_A>>, out1<Eventbean<SupportBean_B>> {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'DefaultSupportSourceOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

        // two incompatible output streams: underlying versus eventbean
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "DefaultSupportSourceOp -> out1<SupportBean_B> {}\n" +
                "MyTestOp(out1) {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': For operator 'MyTestOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

        // incompatible on-input method
        epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "MySBInputOp(out1) {}";
        SupportDataFlowAssertionUtil.tryInvalidInstantiate(epService, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': Failed to find onInput method on for operator 'MySBInputOp#1(out1)' class com.espertech.esper.regression.dataflow.ExecDataflowInvalidGraph$MySBInputOp, expected an onInput method that takes any of {Object, Object[");

        // same schema defined twice
        epl = "create dataflow MyGraph " +
                "create schema ABC (c0 string), create schema ABC (c1 string), " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}";
        SupportDataFlowAssertionUtil.tryInvalidCreate(epService, epl,
                "Error starting statement: Schema name 'ABC' is declared more then once [");
    }

    public static class MyInvalidOpFactory implements DataFlowOperatorFactory {
        public Object create() {
            throw new RuntimeException("Failed-Here");
        }
    }

    @DataFlowOperator
    public static class MyTestOp {

        @DataFlowOpParameter
        private String theString;

        public void onInput(Object o) {
        }
    }

    @DataFlowOperator
    public static class MySBInputOp {
        public void onInput(SupportBean_B b) {
        }
    }
}
