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

import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.*;

import static org.junit.Assert.*;

public class EPLDataflowAPIInstantiationOptions {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowParameterInjectionCallback());
        execs.add(new EPLDataflowOperatorInjectionCallback());
        return execs;
    }

    private static class EPLDataflowParameterInjectionCallback implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne MyOp -> outstream<SomeType> {propOne:'abc', propThree:'xyz'}", path);

            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            MyParameterProvider myParameterProvider = new MyParameterProvider(Collections.<String, Object>singletonMap("propTwo", "def"));
            options.parameterProvider(myParameterProvider);

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);
            MyOp myOp = MyOp.getAndClearInstances().get(0);
            assertEquals("abc", myOp.getPropOne());
            assertEquals("def", myOp.getPropTwo());

            assertEquals(3, myParameterProvider.contextMap.size());
            assertNotNull(myParameterProvider.contextMap.get("propOne"));

            EPDataFlowOperatorParameterProviderContext context = myParameterProvider.contextMap.get("propTwo");
            assertEquals("propTwo", context.getParameterName());
            assertEquals("MyOp", context.getOperatorName());
            assertSame(myOp.getFactory(), context.getFactory());
            assertEquals(0, context.getOperatorNum());
            assertEquals("MyDataFlowOne", context.getDataFlowName());

            context = myParameterProvider.contextMap.get("propThree");
            assertEquals("propThree", context.getParameterName());
            assertEquals("MyOp", context.getOperatorName());
            assertSame(myOp.factory, context.getFactory());
            assertEquals(0, context.getOperatorNum());

            env.undeployAll();
        }
    }

    private static class EPLDataflowOperatorInjectionCallback implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create schema SomeType ()", path);
            env.compileDeploy("@name('flow') create dataflow MyDataFlowOne MyOp -> outstream<SomeType> {propOne:'abc', propThree:'xyz'}", path);

            MyOperatorProvider myOperatorProvider = new MyOperatorProvider();
            EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
            options.operatorProvider(myOperatorProvider);

            env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlowOne", options);

            assertEquals(1, myOperatorProvider.contextMap.size());
            EPDataFlowOperatorProviderContext context = myOperatorProvider.contextMap.get("MyOp");
            assertEquals("MyOp", context.getOperatorName());
            assertEquals("MyDataFlowOne", context.getDataFlowName());

            env.undeployAll();
        }
    }

    public static class MyOpForge implements DataFlowOperatorForge {

        @DataFlowOpParameter
        private ExprNode propOne;

        @DataFlowOpParameter
        private ExprNode propTwo;

        @DataFlowOpParameter
        private ExprNode propThree;

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return new SAIFFInitializeBuilder(MyOpFactory.class, this.getClass(), "myop", parent, symbols, classScope)
                .exprnode("propOne", propOne)
                .exprnode("propTwo", propTwo)
                .exprnode("propThree", propThree)
                .build();
        }
    }

    public static class MyOpFactory implements DataFlowOperatorFactory {
        private ExprEvaluator propOne;
        private ExprEvaluator propTwo;
        private ExprEvaluator propThree;

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            String propOneText = DataFlowParameterResolution.resolveStringOptional("propOne", propOne, context);
            String propTwoText = DataFlowParameterResolution.resolveStringOptional("propTwo", propTwo, context);
            String propThreeText = DataFlowParameterResolution.resolveStringOptional("propThree", propThree, context);
            return new MyOp(this, propOneText, propTwoText, propThreeText);
        }

        public void setPropOne(ExprEvaluator propOne) {
            this.propOne = propOne;
        }

        public void setPropTwo(ExprEvaluator propTwo) {
            this.propTwo = propTwo;
        }

        public void setPropThree(ExprEvaluator propThree) {
            this.propThree = propThree;
        }
    }

    public static class MyOp implements DataFlowSourceOperator {

        private final static List<MyOp> INSTANCES = new ArrayList<>();

        private final MyOpFactory factory;
        private final String propOne;
        private final String propTwo;
        private final String propThree;

        public MyOp(MyOpFactory factory, String propOne, String propTwo, String propThree) {
            this.factory = factory;
            this.propOne = propOne;
            this.propTwo = propTwo;
            this.propThree = propThree;
            INSTANCES.add(this);
        }

        public static List<MyOp> getAndClearInstances() {
            List<MyOp> ops = new ArrayList<>(INSTANCES);
            INSTANCES.clear();
            return ops;
        }

        public void next() throws InterruptedException {
        }

        public MyOpFactory getFactory() {
            return factory;
        }

        public String getPropOne() {
            return propOne;
        }

        public String getPropTwo() {
            return propTwo;
        }

        public String getPropThree() {
            return propThree;
        }

        public void open(DataFlowOpOpenContext openContext) {
        }

        public void close(DataFlowOpCloseContext openContext) {
        }
    }

    public static class MyParameterProvider implements EPDataFlowOperatorParameterProvider {

        private Map<String, EPDataFlowOperatorParameterProviderContext> contextMap = new HashMap<String, EPDataFlowOperatorParameterProviderContext>();
        private final Map<String, Object> values;

        public MyParameterProvider(Map<String, Object> values) {
            this.values = values;
        }

        public Object provide(EPDataFlowOperatorParameterProviderContext context) {
            contextMap.put(context.getParameterName(), context);
            return values.get(context.getParameterName());
        }
    }

    public static class MyOperatorProvider implements EPDataFlowOperatorProvider {
        private Map<String, EPDataFlowOperatorProviderContext> contextMap = new HashMap<String, EPDataFlowOperatorProviderContext>();

        public Object provide(EPDataFlowOperatorProviderContext context) {
            contextMap.put(context.getOperatorName(), context);
            return new MyOp(null, "test", "test", "test");
        }
    }
}
