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
import com.espertech.esper.client.dataflow.*;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.util.DefaultSupportGraphOpProvider;
import com.espertech.esper.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExecDataflowAPIExceptions implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        MyExceptionHandler.getContexts().clear();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        // test exception by graph source
        EPStatement stmtGraph = epService.getEPAdministrator().createEPL("create dataflow MyDataFlow DefaultSupportSourceOp -> outstream<SupportBean> {}");

        DefaultSupportSourceOp op = new DefaultSupportSourceOp(new Object[]{new RuntimeException("My-Exception-Is-Here")});
        EPDataFlowInstantiationOptions options = new EPDataFlowInstantiationOptions();
        options.operatorProvider(new DefaultSupportGraphOpProvider(op));
        MyExceptionHandler handler = new MyExceptionHandler();
        options.exceptionHandler(handler);
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", options);

        df.start();
        Thread.sleep(100);
        assertEquals(EPDataFlowState.COMPLETE, df.getState());

        assertEquals(1, MyExceptionHandler.getContexts().size());
        EPDataFlowExceptionContext context = MyExceptionHandler.getContexts().get(0);
        assertEquals("MyDataFlow", context.getDataFlowName());
        assertEquals("DefaultSupportSourceOp", context.getOperatorName());
        assertEquals(0, context.getOperatorNumber());
        assertEquals("DefaultSupportSourceOp#0() -> outstream<SupportBean>", context.getOperatorPrettyPrint());
        assertEquals("Support-graph-source generated exception: My-Exception-Is-Here", context.getThrowable().getMessage());
        df.cancel();
        stmtGraph.destroy();
        MyExceptionHandler.getContexts().clear();

        // test exception by operator
        epService.getEPAdministrator().getConfiguration().addImport(MyExceptionOp.class);
        epService.getEPAdministrator().createEPL("create dataflow MyDataFlow DefaultSupportSourceOp -> outstream<SupportBean> {}" +
                "MyExceptionOp(outstream) {}");

        DefaultSupportSourceOp opTwo = new DefaultSupportSourceOp(new Object[]{new SupportBean("E1", 1)});
        EPDataFlowInstantiationOptions optionsTwo = new EPDataFlowInstantiationOptions();
        optionsTwo.operatorProvider(new DefaultSupportGraphOpProvider(opTwo));
        MyExceptionHandler handlerTwo = new MyExceptionHandler();
        optionsTwo.exceptionHandler(handlerTwo);
        EPDataFlowInstance dfTwo = epService.getEPRuntime().getDataFlowRuntime().instantiate("MyDataFlow", optionsTwo);

        dfTwo.start();
        Thread.sleep(100);

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

    @DataFlowOperator
    public static class MyExceptionOp {
        public void onInput(SupportBean bean) {
            throw new RuntimeException("Operator-thrown-exception");
        }
    }
}
