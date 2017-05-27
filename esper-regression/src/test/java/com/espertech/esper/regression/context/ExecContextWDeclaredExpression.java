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
package com.espertech.esper.regression.context;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecContextWDeclaredExpression implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionDeclaredExpression(epService);
        runAssertionAliasExpression(epService);
        runAssertionContextFilter(epService);
    }

    private void runAssertionDeclaredExpression(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean");
        epService.getEPAdministrator().createEPL("create expression getLabelOne { context.label }");
        epService.getEPAdministrator().createEPL("create expression getLabelTwo { 'x'||context.label||'x' }");

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("expression getLabelThree { context.label } " +
                "context MyCtx " +
                "select getLabelOne() as c0, getLabelTwo() as c1, getLabelThree() as c2 from SupportBean").addListener(listener);

        tryAssertionExpression(epService, listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAliasExpression(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyCtx as " +
                "group by intPrimitive < 0 as n, " +
                "group by intPrimitive > 0 as p " +
                "from SupportBean");
        epService.getEPAdministrator().createEPL("create expression getLabelOne alias for { context.label }");
        epService.getEPAdministrator().createEPL("create expression getLabelTwo alias for { 'x'||context.label||'x' }");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("expression getLabelThree alias for { context.label } " +
                "context MyCtx " +
                "select getLabelOne as c0, getLabelTwo as c1, getLabelThree as c2 from SupportBean").addListener(listener);

        tryAssertionExpression(epService, listener);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextFilter(EPServiceProvider epService) {
        String expr = "create expression THE_EXPRESSION alias for {theString='x'}";
        epService.getEPAdministrator().createEPL(expr);

        String context = "create context context2 initiated @now and pattern[every(SupportBean(THE_EXPRESSION))] terminated after 10 minutes";
        epService.getEPAdministrator().createEPL(context);

        SupportUpdateListener listener = new SupportUpdateListener();
        String statement = "context context2 select * from pattern[e1=SupportBean(THE_EXPRESSION) -> e2=SupportBean(theString='y')]";
        epService.getEPAdministrator().createEPL(statement).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("x", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("y", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "e1.intPrimitive,e2.intPrimitive".split(","), new Object[]{1, 2});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionExpression(EPServiceProvider epService, SupportUpdateListener listener) {
        String[] fields = "c0,c1,c2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"n", "xnx", "n"});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"p", "xpx", "p"});
    }
}
