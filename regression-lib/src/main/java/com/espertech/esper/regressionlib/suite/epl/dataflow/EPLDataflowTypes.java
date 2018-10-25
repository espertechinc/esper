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

import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstance;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowInstantiationOptions;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.dataflow.SupportGenericOutputOpWPort;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class EPLDataflowTypes {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowBeanType());
        execs.add(new EPLDataflowMapType());
        return execs;
    }

    private static class EPLDataflowBeanType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<SupportBean> {}" +
                "MySupportBeanOutputOp(outstream) {}" +
                "SupportGenericOutputOpWPort(outstream) {}");

            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{new SupportBean("E1", 1)});
            MySupportBeanOutputOp outputOne = new MySupportBeanOutputOp();
            SupportGenericOutputOpWPort<SupportBean> outputTwo = new SupportGenericOutputOpWPort<>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, outputOne, outputTwo));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            dfOne.run();

            SupportBean.compare(outputOne.getAndReset().toArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
            Pair<List<SupportBean>, List<Integer>> received = outputTwo.getAndReset();
            SupportBean.compare(received.getFirst().toArray(), "theString,intPrimitive".split(","), new Object[][]{{"E1", 1}});
            EPAssertionUtil.assertEqualsExactOrder(new Integer[]{0}, received.getSecond().toArray());

            env.undeployAll();
        }
    }

    private static class EPLDataflowMapType implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create map schema MyMap (p0 String, p1 int)", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne " +
                "DefaultSupportSourceOp -> outstream<MyMap> {}" +
                "MyMapOutputOp(outstream) {}" +
                "DefaultSupportCaptureOp(outstream) {}", path);

            DefaultSupportSourceOp source = new DefaultSupportSourceOp(new Object[]{makeMap("E1", 1)});
            MyMapOutputOp outputOne = new MyMapOutputOp();
            DefaultSupportCaptureOp<SupportBean> outputTwo = new DefaultSupportCaptureOp<SupportBean>();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions().operatorProvider(new DefaultSupportGraphOpProvider(source, outputOne, outputTwo));
            EPDataFlowInstance dfOne = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            dfOne.run();

            EPAssertionUtil.assertPropsPerRow(outputOne.getAndReset().toArray(), "p0,p1".split(","), new Object[][]{{"E1", 1}});
            EPAssertionUtil.assertPropsPerRow(outputTwo.getAndReset().get(0).toArray(), "p0,p1".split(","), new Object[][]{{"E1", 1}});

            env.undeployAll();
        }
    }

    private static Map<String, Object> makeMap(String p0, int p1) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("p0", p0);
        map.put("p1", p1);
        return map;
    }

    public static class MySupportBeanOutputOp implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {
        private List<SupportBean> received = new ArrayList<SupportBean>();

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MySupportBeanOutputOp.class);
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {

        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MySupportBeanOutputOp();
        }

        public synchronized void onInput(SupportBean event) {
            received.add(event);
        }

        public synchronized List<SupportBean> getAndReset() {
            List<SupportBean> result = received;
            received = new ArrayList<SupportBean>();
            return result;
        }
    }

    public static class MyMapOutputOp implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {
        private List<Map<String, Object>> received = new ArrayList<Map<String, Object>>();

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MyMapOutputOp.class);
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {

        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MyMapOutputOp();
        }

        public synchronized void onInput(Map<String, Object> event) {
            received.add(event);
        }

        public synchronized List<Map<String, Object>> getAndReset() {
            List<Map<String, Object>> result = received;
            received = new ArrayList<Map<String, Object>>();
            return result;
        }
    }
}
