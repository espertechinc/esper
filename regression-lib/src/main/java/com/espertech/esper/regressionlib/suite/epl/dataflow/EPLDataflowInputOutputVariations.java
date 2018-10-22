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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProviderByOpName;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static org.junit.Assert.assertEquals;

public class EPLDataflowInputOutputVariations {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowLargeNumOpsDataFlow());
        execs.add(new EPLDataflowFanInOut());
        execs.add(new EPLDataflowFactorial());
        return execs;
    }

    private static class EPLDataflowLargeNumOpsDataFlow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            if (env.isHA()) {
                return;
            }
            String epl = "@name('flow') create dataflow MyGraph \n" +
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
                "DefaultSupportCaptureOp(out_17) {}\n";
            env.compileDeploy(epl);

            DefaultSupportCaptureOp<Object> futureOneA = new DefaultSupportCaptureOp<Object>(1);
            Map<String, Object> operators = new HashMap<String, Object>();
            operators.put("DefaultSupportCaptureOp", futureOneA);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProviderByOpName(operators));

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyGraph", options).start();

            Object[] result;
            try {
                result = futureOneA.get(3, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"A1"}}, result);

            env.undeployAll();
        }
    }

    private static class EPLDataflowFanInOut implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('flow') create dataflow MultiInMultiOutGraph \n" +
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
                "DefaultSupportCaptureOp(OutOne) { name : 'SupportOpCountFutureOneA' }\n" +
                "DefaultSupportCaptureOp(OutOne) { name : 'SupportOpCountFutureOneB' }\n" +
                "DefaultSupportCaptureOp(OutTwo) { name : 'SupportOpCountFutureTwoA' }\n" +
                "DefaultSupportCaptureOp(OutTwo) { name : 'SupportOpCountFutureTwoB' }\n";
            env.compileDeploy(epl);

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

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MultiInMultiOutGraph", options).start();

            try {
                EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S1-10"}, {"S1-20"}}, futureOneA.get(3, TimeUnit.SECONDS));
                EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S1-10"}, {"S1-20"}}, futureOneB.get(3, TimeUnit.SECONDS));
                EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S0-A1"}, {"S0-A2"}}, futureTwoA.get(3, TimeUnit.SECONDS));
                EPAssertionUtil.assertEqualsAnyOrder(new Object[][]{{"S0-A1"}, {"S0-A2"}}, futureTwoB.get(3, TimeUnit.SECONDS));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            env.undeployAll();
        }
    }

    private static class EPLDataflowFactorial implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String epl = "@name('flow') create dataflow FactorialGraph \n" +
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
            env.compileDeploy(epl);

            DefaultSupportCaptureOp<Object> future = new DefaultSupportCaptureOp<Object>(1);
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions()
                .operatorProvider(new DefaultSupportGraphOpProvider(future));

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "FactorialGraph", options).start();

            Object[] result;
            try {
                result = future.get(3, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            assertEquals(1, result.length);
            assertEquals((long) 5 * 4 * 3 * 2, ((Object[]) result[0])[0]);

            env.undeployAll();
        }
    }

    public static class MyFactorialOp implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MyFactorialOp.class);
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MyFactorialOp();
        }

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

    public static class MyCustomOp implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {

        @DataFlowContext
        private EPDataFlowEmitter graphContext;

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MyCustomOp.class);
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MyCustomOp();
        }

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
