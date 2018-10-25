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

import com.espertech.esper.common.client.dataflow.core.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib.sleep;
import static org.junit.Assert.assertEquals;

public class EPLDataflowAPIExceptions implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        MyExceptionHandler.getContexts().clear();

        // test exception by graph source
        env.compileDeploy("@name('flow') create dataflow MyDataFlow DefaultSupportSourceOp -> outstream<SupportBean> {}");

        DefaultSupportSourceOp op = new DefaultSupportSourceOp(new Object[]{new RuntimeException("My-Exception-Is-Here")});
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(op));
        MyExceptionHandler handler = new MyExceptionHandler();
        options.exceptionHandler(handler);
        EPDataFlowInstance df = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", options);

        df.start();
        sleep(100);
        assertEquals(EPDataFlowState.COMPLETE, df.getState());

        assertEquals(1, MyExceptionHandler.getContexts().size());
        EPDataFlowExceptionContext context = MyExceptionHandler.getContexts().get(0);
        assertEquals("MyDataFlow", context.getDataFlowName());
        assertEquals("DefaultSupportSourceOp", context.getOperatorName());
        assertEquals(0, context.getOperatorNumber());
        assertEquals("DefaultSupportSourceOp#0() -> outstream<SupportBean>", context.getOperatorPrettyPrint());
        assertEquals("Support-graph-source generated exception: My-Exception-Is-Here", context.getThrowable().getMessage());
        df.cancel();
        env.undeployModuleContaining("flow");
        MyExceptionHandler.getContexts().clear();

        // test exception by operator
        env.compileDeploy("@name('flow') create dataflow MyDataFlow DefaultSupportSourceOp -> outstream<SupportBean> {}" +
            "MyExceptionOp(outstream) {}");

        DefaultSupportSourceOp opTwo = new DefaultSupportSourceOp(new Object[]{new SupportBean("E1", 1)});
        EPDataFlowInstantiationOptions optionsTwo = new EPDataFlowInstantiationOptions();
        optionsTwo.operatorProvider(new DefaultSupportGraphOpProvider(opTwo));
        MyExceptionHandler handlerTwo = new MyExceptionHandler();
        optionsTwo.exceptionHandler(handlerTwo);
        EPDataFlowInstance dfTwo = env.runtime().getDataFlowService().instantiate(env.deploymentId("flow"), "MyDataFlow", optionsTwo);

        dfTwo.start();
        sleep(100);

        assertEquals(1, MyExceptionHandler.getContexts().size());
        EPDataFlowExceptionContext contextTwo = MyExceptionHandler.getContexts().get(0);
        assertEquals("MyDataFlow", contextTwo.getDataFlowName());
        assertEquals("MyExceptionOp", contextTwo.getOperatorName());
        assertEquals(1, contextTwo.getOperatorNumber());
        assertEquals("MyExceptionOp#1(outstream)", contextTwo.getOperatorPrettyPrint());
        assertEquals("Operator-thrown-exception", contextTwo.getThrowable().getMessage());
    }

    public static class MyExceptionHandler implements EPDataFlowExceptionHandler {

        private static List<EPDataFlowExceptionContext> contexts = new ArrayList<EPDataFlowExceptionContext>();

        public static List<EPDataFlowExceptionContext> getContexts() {
            return contexts;
        }

        public static void setContexts(List<EPDataFlowExceptionContext> contexts) {
            MyExceptionHandler.contexts = contexts;
        }

        public void handle(EPDataFlowExceptionContext context) {
            contexts.add(context);
        }
    }

    public static class MyExceptionOpForge implements DataFlowOperatorForge {
        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MyExceptionOpFactory.class);
        }
    }

    public static class MyExceptionOpFactory implements DataFlowOperatorFactory {
        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MyExceptionOp();
        }
    }

    public static class MyExceptionOp implements DataFlowOperator {
        public void onInput(SupportBean bean) {
            throw new RuntimeException("Operator-thrown-exception");
        }
    }
}
