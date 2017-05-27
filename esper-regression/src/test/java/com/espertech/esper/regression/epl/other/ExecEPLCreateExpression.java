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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.*;

public class ExecEPLCreateExpression implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_S0.class);
        configuration.addEventType(SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInvalid(epService);
        runAssertionParseSpecialAndMixedExprAndScript(epService);
        runAssertionExprAndScriptLifecycleAndFilter(epService);
        runAssertionScriptUse(epService);
        runAssertionExpressionUse(epService);
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create expression E1 {''}");
        tryInvalid(epService, "create expression E1 {''}",
                "Error starting statement: Expression 'E1' has already been declared [create expression E1 {''}]");

        epService.getEPAdministrator().createEPL("create expression int js:abc(p1, p2) [p1*p2]");
        tryInvalid(epService, "create expression int js:abc(a, a) [p1*p2]",
                "Error starting statement: Script 'abc' that takes the same number of parameters has already been declared [create expression int js:abc(a, a) [p1*p2]]");
    }

    private void runAssertionParseSpecialAndMixedExprAndScript(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create expression string js:myscript(p1) [\"--\"+p1+\"--\"]");
        epService.getEPAdministrator().createEPL("create expression myexpr {sb => '--'||theString||'--'}");

        // test mapped property syntax
        String eplMapped = "select myscript('x') as c0, myexpr(sb) as c1 from SupportBean as sb";
        EPStatement stmtMapped = epService.getEPAdministrator().createEPL(eplMapped);
        stmtMapped.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{"--x--", "--E1--"});
        stmtMapped.destroy();

        // test expression chained syntax
        String eplExpr = "" +
                "create expression scalarfilter {s => " +
                "   strvals.where(y => y != 'E1') " +
                "}";
        epService.getEPAdministrator().createEPL(eplExpr);
        String eplSelect = "select scalarfilter(t).where(x => x != 'E2') as val1 from SupportCollection as t";
        epService.getEPAdministrator().createEPL(eplSelect).addListener(listener);
        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1,E2,E3,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val1", "E3", "E4");
        epService.getEPAdministrator().destroyAllStatements();
        listener.reset();

        // test script chained synax
        String eplScript = "create expression " + SupportBean.class.getName() + " js:callIt() [ new " + SupportBean.class.getName() + "('E1', 10); ]";
        epService.getEPAdministrator().createEPL(eplScript);
        epService.getEPAdministrator().createEPL("select callIt() as val0, callIt().getTheString() as val1 from SupportBean as sb").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.theString,val0.intPrimitive,val1".split(","), new Object[]{"E1", 10, "E1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionScriptUse(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create expression int js:abc(p1, p2) [p1*p2*10]");
        epService.getEPAdministrator().createEPL("create expression int js:abc(p1) [p1*10]");

        String epl = "select abc(intPrimitive, doublePrimitive) as c0, abc(intPrimitive) as c1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBean("E1", 10, 3.5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{350, 100});

        stmt.destroy();

        // test SODA
        String eplExpr = "create expression somescript(i1) ['a']";
        EPStatementObjectModel modelExpr = epService.getEPAdministrator().compileEPL(eplExpr);
        assertEquals(eplExpr, modelExpr.toEPL());
        EPStatement stmtSODAExpr = epService.getEPAdministrator().create(modelExpr);
        assertEquals(eplExpr, stmtSODAExpr.getText());

        String eplSelect = "select somescript(1) from SupportBean";
        EPStatementObjectModel modelSelect = epService.getEPAdministrator().compileEPL(eplSelect);
        assertEquals(eplSelect, modelSelect.toEPL());
        EPStatement stmtSODASelect = epService.getEPAdministrator().create(modelSelect);
        assertEquals(eplSelect, stmtSODASelect.getText());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionExpressionUse(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create expression TwoPi {Math.PI * 2}");
        epService.getEPAdministrator().createEPL("create expression factorPi {sb => Math.PI * intPrimitive}");

        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "TwoPi() as c0," +
                "(select TwoPi() from SupportBean_S0#lastevent) as c1," +
                "factorPi(sb) as c2 " +
                "from SupportBean sb";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(epl);
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));   // factor is 3
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields,
                new Object[]{Math.PI * 2, Math.PI * 2, Math.PI * 3});

        stmtSelect.destroy();

        // test local expression override
        EPStatement stmtOverride = epService.getEPAdministrator().createEPL("expression TwoPi {Math.PI * 10} select TwoPi() as c0 from SupportBean");
        stmtOverride.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{Math.PI * 10});

        // test SODA
        String eplExpr = "create expression JoinMultiplication {(s1,s2) => s1.intPrimitive*s2.id}";
        EPStatementObjectModel modelExpr = epService.getEPAdministrator().compileEPL(eplExpr);
        assertEquals(eplExpr, modelExpr.toEPL());
        EPStatement stmtSODAExpr = epService.getEPAdministrator().create(modelExpr);
        assertEquals(eplExpr, stmtSODAExpr.getText());

        // test SODA and join and 2-stream parameter
        String eplJoin = "select JoinMultiplication(sb,s0) from SupportBean#lastevent as sb, SupportBean_S0#lastevent as s0";
        EPStatementObjectModel modelJoin = epService.getEPAdministrator().compileEPL(eplJoin);
        assertEquals(eplJoin, modelJoin.toEPL());
        EPStatement stmtSODAJoin = epService.getEPAdministrator().create(modelJoin);
        assertEquals(eplJoin, stmtSODAJoin.getText());
        epService.getEPAdministrator().destroyAllStatements();

        // test subquery against named window and table defined in declared expression
        tryAssertionTestExpressionUse(epService, true);
        tryAssertionTestExpressionUse(epService, false);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionTestExpressionUse(EPServiceProvider epService, boolean namedWindow) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create expression myexpr {(select intPrimitive from MyInfra)}");
        String eplCreate = namedWindow ?
                "create window MyInfra#keepall as SupportBean" :
                "create table MyInfra(theString string, intPrimitive int)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select theString, intPrimitive from SupportBean");
        epService.getEPAdministrator().createEPL("select myexpr() as c0 from SupportBean_S0").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 100));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{100});
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }

    private void runAssertionExprAndScriptLifecycleAndFilter(EPServiceProvider epService) {
        // expression assertion
        tryAssertionLifecycleAndFilter(epService, "create expression MyFilter {sb => intPrimitive = 1}",
                "select * from SupportBean(MyFilter(sb)) as sb",
                "create expression MyFilter {sb => intPrimitive = 2}");

        // script assertion
        tryAssertionLifecycleAndFilter(epService, "create expression boolean js:MyFilter(intPrimitive) [intPrimitive==1]",
                "select * from SupportBean(MyFilter(intPrimitive)) as sb",
                "create expression boolean js:MyFilter(intPrimitive) [intPrimitive==2]");
    }

    private void tryAssertionLifecycleAndFilter(EPServiceProvider epService, String expressionBefore,
                                                String selector,
                                                String expressionAfter) {
        SupportUpdateListener l1 = new SupportUpdateListener();
        SupportUpdateListener l2 = new SupportUpdateListener();

        EPStatement stmtExpression = epService.getEPAdministrator().createEPL(expressionBefore);

        EPStatement stmtSelectOne = epService.getEPAdministrator().createEPL(selector);
        stmtSelectOne.addListener(l1);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertFalse(l1.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertTrue(l1.getAndClearIsInvoked());

        stmtExpression.destroy();
        epService.getEPAdministrator().createEPL(expressionAfter);

        EPStatement stmtSelectTwo = epService.getEPAdministrator().createEPL(selector);
        stmtSelectTwo.addListener(l2);

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 0));
        assertFalse(l1.getAndClearIsInvoked() || l2.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        assertTrue(l1.getAndClearIsInvoked());
        assertFalse(l2.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E4", 2));
        assertFalse(l1.getAndClearIsInvoked());
        assertTrue(l2.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean makeBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean();
        sb.setIntPrimitive(intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }
}
