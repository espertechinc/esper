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
package com.espertech.esper.regressionlib.suite.client.extension;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.enummethod.*;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPInputEnum;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPParam;
import com.espertech.esper.common.internal.epl.util.EPLExpressionParamType;
import com.espertech.esper.common.internal.rettype.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.util.LambdaAssertionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClientExtendEnumMethod {
    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientExtendEnumScalarNoParamMedian());
        execs.add(new ClientExtendEnumEventLambdaMedian());
        execs.add(new ClientExtendEnumScalarLambdaMedian());
        execs.add(new ClientExtendEnumScalarNoLambdaWithParams());
        execs.add(new ClientExtendEnumScalarEarlyExit());
        execs.add(new ClientExtendEnumPredicateReturnEvents());
        execs.add(new ClientExtendEnumPredicateReturnSingleEvent());
        execs.add(new ClientExtendEnumTwoLambdaParameters());
        execs.add(new ClientExtendEnumLambdaEventInputValueAndIndex());
        execs.add(new ClientExtendEnumLambdaScalarInputValueAndIndex());
        execs.add(new ClientExtendEnumLambdaScalarStateAndValue());
        return execs;
    }

    private static class ClientExtendEnumLambdaScalarStateAndValue implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select strvals.enumPlugInLambdaScalarWStateAndValue('X', (r, v) => r || v) as c0 " +
                "from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, "Xa", "a");
            sendAssert(env, "Xab", "a,b");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String expected, String csv) {
            env.sendEventBean(SupportCollection.makeString(csv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
        }
    }

    private static class ClientExtendEnumLambdaScalarInputValueAndIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intvals.enumPlugInLambdaScalarWPredicateAndIndex((v, ind) => v > 0 and ind < 3) as c0 " +
                "from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, 0, "-1,-2");
            sendAssert(env, 1, "-1,2");
            sendAssert(env, 2, "2,-1,2");
            sendAssert(env, 3, "2,2,2");
            sendAssert(env, 3, "2,2,2,2");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, Integer expected, String csv) {
            env.sendEventBean(SupportCollection.makeNumeric(csv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
        }
    }

    private static class ClientExtendEnumLambdaEventInputValueAndIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(select * from SupportBean#keepall).enumPlugInLambdaEventWPredicateAndIndex((v, ind) => v.intPrimitive > 0 and ind < 3) as c0 " +
                "from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E1", -1));
            sendAssert(env, 0);

            env.sendEventBean(new SupportBean("E2", 10));
            sendAssert(env, 1);

            env.sendEventBean(new SupportBean("E3", 20));
            sendAssert(env, 2);

            env.sendEventBean(new SupportBean("E4", 3));
            sendAssert(env, 2);

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, Integer expected) {
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class ClientExtendEnumTwoLambdaParameters implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(select * from SupportBean#keepall).enumPlugInTwoLambda(l1 -> 2*intPrimitive, l2 -> 3*intPrimitive) as c0 " +
                "from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E1", 0));
            sendAssert(env, 0);

            env.sendEventBean(new SupportBean("E2", 2));
            sendAssert(env, 10);

            env.sendEventBean(new SupportBean("E3", 4));
            sendAssert(env, 30);

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, Integer expected) {
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class ClientExtendEnumPredicateReturnSingleEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(select * from SupportBean#keepall).enumPlugInReturnSingleEvent(v => intPrimitive > 0).theString as c0 " +
                "from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E1", -1));
            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E2", 1));
            sendAssert(env, "E2");

            env.sendEventBean(new SupportBean("E3", 0));
            sendAssert(env, "E2");

            env.sendEventBean(new SupportBean("E4", 1));
            sendAssert(env, "E2");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String expected) {
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class ClientExtendEnumPredicateReturnEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select " +
                "(select * from SupportBean#keepall).enumPlugInReturnEvents(v => intPrimitive > 0).lastOf().theString as c0 " +
                "from SupportBean_S0";
            env.compileDeploy(epl).addListener("s0");
            assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("c0"));

            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E1", -1));
            sendAssert(env, null);

            env.sendEventBean(new SupportBean("E2", 1));
            sendAssert(env, "E2");

            env.sendEventBean(new SupportBean("E3", 0));
            sendAssert(env, "E2");

            env.sendEventBean(new SupportBean("E4", 1));
            sendAssert(env, "E4");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String expected) {
            env.sendEventBean(new SupportBean_S0(0));
            assertEquals(expected, env.listener("s0").assertOneGetNewAndReset().get("c0"));
        }
    }

    private static class ClientExtendEnumScalarEarlyExit implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String epl = "@name('s0') select " +
                "intvals.enumPlugInEarlyExit() as val0 " +
                "from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class});

            sendAssert(env, fields, 12, "12,1,1");
            sendAssert(env, fields, 10, "5,5,5");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String[] fields, Integer expected, String csv) {
            env.sendEventBean(SupportCollection.makeNumeric(csv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }

    private static class ClientExtendEnumScalarNoLambdaWithParams implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String epl = "@name('s0') select " +
                "intvals.enumPlugInOne(10, 20) as val0 " +
                "from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Integer.class});

            sendAssert(env, fields, 11, "1,2,11,3");
            sendAssert(env, fields, 0, "");
            sendAssert(env, fields, 23, "11,12");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String[] fields, Integer expected, String csv) {
            env.sendEventBean(SupportCollection.makeNumeric(csv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }

    private static class ClientExtendEnumScalarLambdaMedian implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String epl = "@name('s0') select " +
                "strvals.enumPlugInMedian(v => extractNum(v)) as val0 " +
                "from SupportCollection";
            env.compileDeploy(epl).addListener("s0");
            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Double.class});

            sendAssert(env, fields, 3d, "E2,E1,E5,E4");
            sendAssert(env, fields, null, "E1");
            sendAssert(env, fields, null, "");
            sendAssert(env, fields, null, null);

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String[] fields, Double expected, String csv) {
            env.sendEventBean(SupportCollection.makeString(csv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }

    private static class ClientExtendEnumEventLambdaMedian implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String epl = "@name('s0') select " +
                "contained.enumPlugInMedian(x => p00) as val0 " +
                "from SupportBean_ST0_Container";
            env.compileDeploy(epl).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Double.class});

            sendAssert(env, fields, 11d, "E1,12", "E2,11", "E3,2");
            sendAssert(env, fields, null, null);
            sendAssert(env, fields, null);
            sendAssert(env, fields, 0d, "E1,1", "E2,0", "E3,0");

            env.undeployAll();
        }

        private static void sendAssert(RegressionEnvironment env, String[] fields, Double expected, String... values) {
            env.sendEventBean(SupportBean_ST0_Container.make2Value(values));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }

    private static class ClientExtendEnumScalarNoParamMedian implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "val0".split(",");
            String eplFragment = "@name('s0') select intvals.enumPlugInMedian() as val0 from SupportCollection";
            env.compileDeploy(eplFragment).addListener("s0");

            LambdaAssertionUtil.assertTypes(env.statement("s0").getEventType(), fields, new Class[]{Double.class});

            sendAssert(env, fields, 2d, "1,2,2,4");
            sendAssert(env, fields, 2d, "1,2,2,10");
            sendAssert(env, fields, 2.5d, "1,2,3,4");
            sendAssert(env, fields, 2d, "1,2,2,3,4");
            sendAssert(env, fields, 2d, "1,1,2,2,3,4");
            sendAssert(env, fields, 1d, "1,1");
            sendAssert(env, fields, 1.5d, "1,2");
            sendAssert(env, fields, 2d, "1,3");
            sendAssert(env, fields, 2.5d, "1,4");
            sendAssert(env, fields, null, "1");
            sendAssert(env, fields, null, "");

            env.undeployAll();
        }

        private void sendAssert(RegressionEnvironment env, String[] fields, Double expected, String intcsv) {
            env.sendEventBean(SupportCollection.makeNumeric(intcsv));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{expected});
        }
    }

    public static int extractNum(String arg) {
        return Integer.parseInt(arg.substring(1));
    }

    public static class MyLocalEnumMethodForgeMedian implements EnumMethodForgeFactory {
        public static final DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_NUMERIC),
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY, new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.NUMERIC)),
            new DotMethodFP(DotMethodFPInputEnum.EVENTCOLL, new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.NUMERIC))
        };

        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            return new EnumMethodDescriptor(FOOTPRINTS);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            Class stateClass = MyLocalEnumMethodMedianState.class; // the class providing state
            Class serviceClass = MyLocalEnumMethodMedianService.class; // the class providing the processing method
            String methodName = "next"; // the name of the method for processing an item of input values
            EPType returnType = new ClassEPType(Double.class); // indicate that we are returning a Double-type value
            boolean earlyExit = false;
            return new EnumMethodModeStaticMethod(stateClass, serviceClass, methodName, returnType, earlyExit);
        }
    }

    public static class MyLocalEnumMethodMedianState implements EnumMethodState {
        private List<Integer> list = new ArrayList<>();

        public Object state() {
            Collections.sort(list);
            // get count of scores
            int totalElements = list.size();
            if (totalElements < 2) {
                return null;
            }
            // check if total number of scores is even
            if (totalElements % 2 == 0) {
                int sumOfMiddleElements = list.get(totalElements / 2) + list.get(totalElements / 2 - 1);
                // calculate average of middle elements
                return ((double) sumOfMiddleElements) / 2;
            }
            return (double) list.get(totalElements / 2);
        }

        public void add(Integer value) {
            list.add(value);
        }
    }

    public static class MyLocalEnumMethodMedianService {
        public static void next(MyLocalEnumMethodMedianState state, Object element, Object valueSelectorResult) {
            state.add((Integer) valueSelectorResult);
        }

        public static void next(MyLocalEnumMethodMedianState state, Object element) {
            state.add((Integer) element);
        }
    }

    public static class MyLocalEnumMethodForgeOne implements EnumMethodForgeFactory {
        public static final DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_NUMERIC,
                new DotMethodFPParam("from", EPLExpressionParamType.NUMERIC),
                new DotMethodFPParam("to", EPLExpressionParamType.NUMERIC))
        };

        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            return new EnumMethodDescriptor(FOOTPRINTS);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            return new EnumMethodModeStaticMethod(MyLocalEnumMethodForgeOneState.class, MyLocalEnumMethodForgeOneState.class, "next", new ClassEPType(Integer.class), false);
        }
    }

    public static class MyLocalEnumMethodForgeOneState implements EnumMethodState {
        private int from;
        private int to;
        private int sum;

        public void setParameter(int parameterNumber, Object value) {
            if (parameterNumber == 0) {
                from = (Integer) value;
            }
            if (parameterNumber == 1) {
                to = (Integer) value;
            }
        }

        public static void next(MyLocalEnumMethodForgeOneState state, Object num) {
            state.add((Integer) num);
        }

        public void add(int value) {
            if (value >= from && value <= to) {
                sum += value;
            }
        }

        public Object state() {
            return sum;
        }
    }

    public static class MyLocalEnumMethodForgeEarlyExit implements EnumMethodForgeFactory {
        public static final DotMethodFP[] FOOTPRINTS = new DotMethodFP[]{
            new DotMethodFP(DotMethodFPInputEnum.SCALAR_NUMERIC)
        };

        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            return new EnumMethodDescriptor(FOOTPRINTS);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            return new EnumMethodModeStaticMethod(MyLocalEnumMethodForgeEarlyExitState.class, MyLocalEnumMethodForgeEarlyExitState.class, "next", new ClassEPType(Integer.class), true);
        }
    }

    public static class MyLocalEnumMethodForgeEarlyExitState implements EnumMethodState {
        private int sum;

        public static void next(MyLocalEnumMethodForgeEarlyExitState state, Object num) {
            state.sum += (Integer) num;
        }

        public boolean completed() {
            return sum >= 10;
        }

        public Object state() {
            return sum;
        }
    }

    public static class MyLocalEnumMethodForgePredicateReturnEvents implements EnumMethodForgeFactory {
        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            DotMethodFP[] footprints = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.EVENTCOLL, new DotMethodFPParam(1, "predicate", EPLExpressionParamType.BOOLEAN))
            };
            return new EnumMethodDescriptor(footprints);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            EPType type = EPTypeHelper.collectionOfEvents(context.getInputEventType());
            return new EnumMethodModeStaticMethod(MyLocalEnumMethodForgePredicateReturnEventsState.class, MyLocalEnumMethodForgePredicateReturnEvents.class, "next", type, false);
        }

        public static void next(MyLocalEnumMethodForgePredicateReturnEventsState state, EventBean event, boolean pass) {
            if (pass) {
                state.add(event);
            }
        }
    }

    public static class MyLocalEnumMethodForgePredicateReturnEventsState implements EnumMethodState {
        List<EventBean> events = new ArrayList<>();

        public Object state() {
            return events;
        }

        public void add(EventBean event) {
            events.add(event);
        }
    }

    public static class MyLocalEnumMethodForgePredicateReturnSingleEvent implements EnumMethodForgeFactory {
        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            DotMethodFP[] footprints = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.EVENTCOLL, new DotMethodFPParam(1, "predicate", EPLExpressionParamType.BOOLEAN))
            };
            return new EnumMethodDescriptor(footprints);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            EventEPType type = new EventEPType(context.getInputEventType());
            return new EnumMethodModeStaticMethod(MyLocalEnumMethodForgePredicateReturnSingleEventState.class, MyLocalEnumMethodForgePredicateReturnSingleEvent.class, "next", type, true);
        }

        public static void next(MyLocalEnumMethodForgePredicateReturnSingleEventState state, EventBean event, Boolean pass) {
            if (pass) {
                state.add(event);
            }
        }
    }

    public static class MyLocalEnumMethodForgePredicateReturnSingleEventState implements EnumMethodState {
        EventBean event;

        public Object state() {
            return event;
        }

        public void add(EventBean event) {
            this.event = event;
        }

        public boolean completed() {
            return event != null;
        }
    }

    public static class MyLocalEnumMethodForgeTwoLambda implements EnumMethodForgeFactory {
        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            DotMethodFP[] footprints = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.EVENTCOLL, new DotMethodFPParam(1, "v1", EPLExpressionParamType.ANY), new DotMethodFPParam(1, "v2", EPLExpressionParamType.ANY))
            };
            return new EnumMethodDescriptor(footprints);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            return new EnumMethodModeStaticMethod(MyLocalEnumMethodForgeTwoLambdaState.class, MyLocalEnumMethodForgeTwoLambdaState.class, "next", new ClassEPType(Integer.class), false);
        }
    }

    public static class MyLocalEnumMethodForgeTwoLambdaState implements EnumMethodState {
        private Integer sum;

        public Object state() {
            return sum;
        }

        public static void next(MyLocalEnumMethodForgeTwoLambdaState state, EventBean event, Object v1, Object v2) {
            state.add((Integer) v1, (Integer) v2);
        }

        void add(Integer v1, Integer v2) {
            if (sum == null) {
                sum = 0;
            }
            sum += v1;
            sum += v2;
        }
    }

    public static class MyLocalEnumMethodForgeThree implements EnumMethodForgeFactory {
        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            DotMethodFP[] footprints = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "value, index", EPLExpressionParamType.BOOLEAN))
            };

            return new EnumMethodDescriptor(footprints);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            EnumMethodModeStaticMethod mode = new EnumMethodModeStaticMethod(MyLocalEnumMethodForgeThreeState.class, MyLocalEnumMethodForgeThree.class, "next", new ClassEPType(Integer.class), false);
            mode.setLambdaParameters(descriptor -> {
                if (descriptor.getLambdaParameterNumber() == 0) {
                    return EnumMethodLambdaParameterTypeValue.INSTANCE;
                }
                return EnumMethodLambdaParameterTypeIndex.INSTANCE;
            });
            return mode;
        }

        public static void next(MyLocalEnumMethodForgeThreeState state, Object value, Boolean pass) {
            if (pass) {
                state.increment();
            }
        }

        public static void next(MyLocalEnumMethodForgeThreeState state, EventBean event, Boolean pass) {
            if (pass) {
                state.increment();
            }
        }
    }

    public static class MyLocalEnumMethodForgeThreeState implements EnumMethodState {
        int count;

        public void increment() {
            count++;
        }

        public Object state() {
            return count;
        }
    }

    public static class MyLocalEnumMethodForgeStateWValue implements EnumMethodForgeFactory {
        public EnumMethodDescriptor initialize(EnumMethodInitializeContext context) {
            DotMethodFP[] footprints = new DotMethodFP[] {
                new DotMethodFP(DotMethodFPInputEnum.ANY,
                    new DotMethodFPParam(0, "initialvalue", EPLExpressionParamType.ANY),
                    new DotMethodFPParam(2, "result, index", EPLExpressionParamType.ANY))
            };
            return new EnumMethodDescriptor(footprints);
        }

        public EnumMethodModeStaticMethod validate(EnumMethodValidateContext context) {
            EnumMethodModeStaticMethod mode = new EnumMethodModeStaticMethod(MyLocalEnumMethodForgeStateWValueState.class, MyLocalEnumMethodForgeStateWValueState.class, "next", new ClassEPType(String.class), false);
            mode.setLambdaParameters(descriptor -> {
                if (descriptor.getLambdaParameterNumber() == 0) {
                    return new EnumMethodLambdaParameterTypeStateGetter(String.class, "getResult");
                }
                return EnumMethodLambdaParameterTypeValue.INSTANCE;
            });
            return mode;
        }
    }

    public static class MyLocalEnumMethodForgeStateWValueState implements EnumMethodState {

        private String result;

        @Override
        public void setParameter(int parameterNumber, Object value) {
            this.result = (String) value;
        }

        public String getResult() {
            return result;
        }

        public static void next(MyLocalEnumMethodForgeStateWValueState state, Object value, String result) {
            state.result = result;
        }

        public Object state() {
            return result;
        }
    }
}
