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
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationOptions;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProviderByOpName;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ExecDataflowInputOutputVariations implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionLargeNumOpsDataFlow(epService);
        runAssertionFanInOut(epService);
        runAssertionFactorial(epService);
    }

    private void runAssertionLargeNumOpsDataFlow(EPServiceProvider epService) throws Exception {
        String epl = "create dataflow MyGraph \n" +
                "" +
                "create objectarray schema SchemaOne (p1 string),\n" +
                "\n" +
                "BeaconSource -> InStream<SchemaOne> {p1:'A1', iterations:1}\n" +
                "Select(InStream) -> out_1 { select: (select p1 from InStream) }\n" +
                "Select(out_1) -> out_2 { select: (select p1 from out_1) }\n" +
                "Select(out_2) -> out_3 { select: (select p1 from out_2) }\n" +
                "Select(out_3) -> out_4 { select: (select p1 from out_3) }\n" +
                "Select(out_4) -> out_5 { select: (select p1 from out_4) }\n" +
                "Select(out_5) -> out_6 { select: (select p1 from out_5) }\n" +
                "Select(out_6) -> out_7 { select: (select p1 from out_6) }\n" +
                "Select(out_7) -> out_8 { select: (select p1 from out_7) }\n" +
                "Select(out_8) -> out_9 { select: (select p1 from out_8) }\n" +
                "Select(out_9) -> out_10 { select: (select p1 from out_9) }\n" +
                "Select(out_10) -> out_11 { select: (select p1 from out_10) }\n" +
                "Select(out_11) -> out_12 { select: (select p1 from out_11) }\n" +
                "Select(out_12) -> out_13 { select: (select p1 from out_12) }\n" +
                "Select(out_13) -> out_14 { select: (select p1 from out_13) }\n" +
                "Select(out_14) -> out_15 { select: (select p1 from out_14) }\n" +
                "Select(out_15) -> out_16 { select: (select p1 from out_15) }\n" +
                "Select(out_16) -> out_17 { select: (select p1 from out_16) }\n" +
                "\n" +
                "SupportOpCountFutureOneA(out_17) {}\n";
        epService.getEPAdministrator().createEPL(epl);

        DefaultSupportCaptureOp<Object> futureOneA = new DefaultSupportCaptureOp<Object>(1);
        Map<String, Object> operators = new HashMap<String, Object>();
        operators.put("SupportOpCountFutureOneA", futureOneA);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph", options).start();

        Object[] result = futureOneA.get(3, TimeUnit.SECONDS);
        EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"A1"}}, result);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFanInOut(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(MyCustomOp.class);

        String epl = "create dataflow MultiInMultiOutGraph \n" +
                "" +
                "create objectarray schema SchemaOne (p1 string),\n" +
                "create objectarray schema SchemaTwo (p2 int),\n" +
                "\n" +
                "BeaconSource -> InOne<SchemaOne> {p1:'A1', iterations:1}\n" +
                "BeaconSource -> InTwo<SchemaOne> {p1:'A2', iterations:1}\n" +
                "\n" +
                "BeaconSource -> InThree<SchemaTwo> {p2:10, iterations:1}\n" +
                "BeaconSource -> InFour<SchemaTwo> {p2:20, iterations:1}\n" +
                "MyCustomOp((InOne, InTwo) as S0, (InThree, InFour) as S1) -> OutOne<SchemaTwo>, OutTwo<SchemaOne>{}\n" +
                "\n" +
                "SupportOpCountFutureOneA(OutOne) {}\n" +
                "SupportOpCountFutureOneB(OutOne) {}\n" +
                "SupportOpCountFutureTwoA(OutTwo) {}\n" +
                "SupportOpCountFutureTwoB(OutTwo) {}\n";
        epService.getEPAdministrator().createEPL(epl);

        DefaultSupportCaptureOp<Object> futureOneA = new DefaultSupportCaptureOp<Object>(2);
        DefaultSupportCaptureOp<Object> futureOneB = new DefaultSupportCaptureOp<Object>(2);
        DefaultSupportCaptureOp<Object> futureTwoA = new DefaultSupportCaptureOp<Object>(2);
        DefaultSupportCaptureOp<Object> futureTwoB = new DefaultSupportCaptureOp<Object>(2);

        Map<String, Object> operators = new HashMap<String, Object>();
        operators.put("SupportOpCountFutureOneA", futureOneA);
        operators.put("SupportOpCountFutureOneB", futureOneB);
        operators.put("SupportOpCountFutureTwoA", futureTwoA);
        operators.put("SupportOpCountFutureTwoB", futureTwoB);

        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));

        epService.getEPRuntime().getDataFlowRuntime().instantiate("MultiInMultiOutGraph", options).start();

        EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S1-10"}, {"S1-20"}}, futureOneA.get(3, TimeUnit.SECONDS));
        EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S1-10"}, {"S1-20"}}, futureOneB.get(3, TimeUnit.SECONDS));
        EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S0-A1"}, {"S0-A2"}}, futureTwoA.get(3, TimeUnit.SECONDS));
        EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S0-A1"}, {"S0-A2"}}, futureTwoB.get(3, TimeUnit.SECONDS));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionFactorial(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addImport(MyFactorialOp.class);

        String epl = "create dataflow FactorialGraph \n" +
                "" +
                "create objectarray schema InputSchema (number int),\n" +
                "create objectarray schema TempSchema (current int, temp long),\n" +
                "create objectarray schema FinalSchema (result long),\n" +
                "\n" +
                "BeaconSource -> InputData<InputSchema> {number:5, iterations:1}\n" +
                "\n" +
                "MyFactorialOp(InputData as Input, TempResult as Temp) -> TempResult<TempSchema>, FinalResult<FinalSchema>{}\n" +
                "\n" +
                "DefaultSupportCaptureOp(FinalResult) {}\n";
        epService.getEPAdministrator().createEPL(epl);

        DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));

        epService.getEPRuntime().getDataFlowRuntime().instantiate("FactorialGraph", options).start();

        Object[] result = future.get(3, TimeUnit.SECONDS);
        assertEquals(1, result.length);
        assertEquals((long) 5 * 4 * 3 * 2, ((Object[]) result[0])[0]);

        epService.getEPAdministrator().destroyAllStatements();
    }

    @DataFlowOperator
    public static class MyFactorialOp {

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        public void onInput(int number) {
            graphContext.submitPort(0, new Object[]{number, (long) number});
        }

        public void onTemp(int current, long temp) {
            if (current == 1) {
                graphContext.submitPort(1, new Object[]{temp});   // we are done
            } else {
                current--;
                long result = temp * current;
                graphContext.submitPort(0, new Object[]{current, result});
            }
        }
    }

    @DataFlowOperator
    public static class MyCustomOp {

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        public void onS0(String value) {
            String output = "S0-" + value;
            graphContext.submitPort(1, new Object[]{output});
        }

        public void onS1(int value) {
            String output = "S1-" + value;
            graphContext.submitPort(0, new Object[]{output});
        }
    }
}
