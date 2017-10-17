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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBeanInt;
import com.espertech.esper.supportregression.epl.SupportJoinMethods;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExecFromClauseMethodNStream implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
        configuration.getEngineDefaults().getByteCodeGeneration().setIncludeDebugSymbols(true);

        configuration.addEventType(SupportBeanInt.class);
        configuration.addImport(SupportJoinMethods.class.getName());
        configuration.addVariable("var1", Integer.class, 0);
        configuration.addVariable("var2", Integer.class, 0);
        configuration.addVariable("var3", Integer.class, 0);
        configuration.addVariable("var4", Integer.class, 0);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertion1Stream2HistStarSubordinateCartesianLast(epService);
        runAssertion1Stream2HistStarSubordinateJoinedKeepall(epService);
        runAssertion1Stream2HistForwardSubordinate(epService);
        runAssertion1Stream3HistStarSubordinateCartesianLast(epService);
        runAssertion1Stream3HistForwardSubordinate(epService);
        runAssertion1Stream3HistChainSubordinate(epService);
        runAssertion2Stream2HistStarSubordinate(epService);
        runAssertion3Stream1HistSubordinate(epService);
        runAssertion3HistPureNoSubordinate(epService);
        runAssertion3Hist1Subordinate(epService);
        runAssertion3Hist2SubordinateChain(epService);
        runAssertion3Stream1HistStreamNWTwice(epService);
    }

    private void runAssertion3Stream1HistStreamNWTwice(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(MySampleTradeEvent.class);

        epService.getEPAdministrator().createEPL("create window AllTrades#keepall as MySampleTradeEvent");
        epService.getEPAdministrator().createEPL("insert into AllTrades select * from MySampleTradeEvent");

        String epl = "select us, them, corr.correlation as crl " +
                "from AllTrades as us, AllTrades as them," +
                "method:" + this.getClass().getName() + ".computeCorrelation(us, them) as corr\n" +
                "where us.side != them.side and corr.correlation > 0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        MySampleTradeEvent one = new MySampleTradeEvent("T1", "B");
        epService.getEPRuntime().sendEvent(one);
        assertFalse(listener.isInvoked());

        MySampleTradeEvent two = new MySampleTradeEvent("T2", "S");
        epService.getEPRuntime().sendEvent(two);

        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "us,them,crl".split(","), new Object[][] {{one, two, 1}, {two, one, 1}});
    }

    private void runAssertion1Stream2HistStarSubordinateCartesianLast(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                "order by h0.val, h1.val";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 1, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H11"}});

        sendBeanInt(epService, "E2", 2, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E3", 0, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E3", 2, 2);
        Object[][] result = new Object[][]{{"E3", "H01", "H11"}, {"E3", "H01", "H12"}, {"E3", "H02", "H11"}, {"E3", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E4", 2, 1);
        result = new Object[][]{{"E4", "H01", "H11"}, {"E4", "H02", "H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion1Stream2HistStarSubordinateJoinedKeepall(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                "where h0.index = h1.index and h0.index = p02";
        tryAssertionOne(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1   from " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#keepall as s0 " +
                "where h0.index = h1.index and h0.index = p02";
        tryAssertionOne(epService, expression);
    }

    private void tryAssertionOne(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 20, 20, 3);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", "H03", "H13"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H03", "H13"}});

        sendBeanInt(epService, "E2", 20, 20, 21);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H03", "H13"}});

        sendBeanInt(epService, "E3", 4, 4, 2);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E3", "H02", "H12"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H03", "H13"}, {"E3", "H02", "H12"}});

        stmt.destroy();
    }

    private void runAssertion1Stream2HistForwardSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(h0.val, p01) as h1 " +
                "order by h0.val, h1.val";
        tryAssertionTwo(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchVal(h0.val, p01) as h1, " +
                "SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                "order by h0.val, h1.val";
        tryAssertionTwo(epService, expression);
    }

    private void tryAssertionTwo(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 1, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H011"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H011"}});

        sendBeanInt(epService, "E2", 0, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H011"}});

        sendBeanInt(epService, "E3", 1, 0);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H011"}});

        sendBeanInt(epService, "E4", 2, 2);
        Object[][] result = {{"E4", "H01", "H011"}, {"E4", "H01", "H012"}, {"E4", "H02", "H021"}, {"E4", "H02", "H022"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(result, new Object[][]{{"E1", "H01", "H011"}}));

        stmt.destroy();
    }

    private void runAssertion1Stream3HistStarSubordinateCartesianLast(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#lastevent as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                "order by h0.val, h1.val, h2.val";
        tryAssertionThree(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#lastevent as s0 " +
                "order by h0.val, h1.val, h2.val";
        tryAssertionThree(epService, expression);
    }

    private void tryAssertionThree(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1,valh2".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 1, 1, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11", "H21"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H11", "H21"}});

        sendBeanInt(epService, "E2", 1, 1, 2);
        Object[][] result = new Object[][]{{"E2", "H01", "H11", "H21"}, {"E2", "H01", "H11", "H22"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion1Stream3HistForwardSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H2', p02) as h2 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";
        tryAssertionFour(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H2', p02) as h2, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";
        tryAssertionFour(epService, expression);
    }

    private void tryAssertionFour(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1,valh2".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 2, 2, 2, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}});

        sendBeanInt(epService, "E2", 4, 4, 4, 3);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E2", "H03", "H13", "H03H23"}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E1", "H01", "H11", "H01H21"}, {"E2", "H03", "H13", "H03H23"}});

        stmt.destroy();
    }

    private void runAssertion1Stream3HistChainSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt#keepall as s0, " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(h0.val||'H1', p01) as h1, " +
                "method:SupportJoinMethods.fetchVal(h1.val||'H2', p02) as h2 " +
                " where h0.index = h1.index and h1.index = h2.index and h2.index = p03";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1,valh2".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E2", 4, 4, 4, 3);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"E2", "H03", "H03H13", "H03H13H23"}});

        sendBeanInt(epService, "E2", 4, 4, 4, 5);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);

        sendBeanInt(epService, "E2", 4, 4, 0, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, null);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"E2", "H03", "H03H13", "H03H13H23"}});

        stmt.destroy();
    }

    private void runAssertion2Stream2HistStarSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as ids0, s1.id as ids1, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'S0%')#keepall as s0, " +
                "SupportBeanInt(id like 'S1%')#lastevent as s1, " +
                "method:SupportJoinMethods.fetchVal(s0.id||'H1', s0.p00) as h0, " +
                "method:SupportJoinMethods.fetchVal(s1.id||'H2', s1.p00) as h1 " +
                "order by s0.id asc";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "ids0,ids1,valh0,valh1".split(",");
        sendBeanInt(epService, "S00", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        assertFalse(listener.isInvoked());

        sendBeanInt(epService, "S10", 1);
        Object[][] resultOne = new Object[][]{{"S00", "S10", "S00H11", "S10H21"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultOne);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "S01", 1);
        Object[][] resultTwo = new Object[][]{{"S01", "S10", "S01H11", "S10H21"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

        sendBeanInt(epService, "S11", 1);
        Object[][] resultThree = new Object[][]{{"S00", "S11", "S00H11", "S11H21"}, {"S01", "S11", "S01H11", "S11H21"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultThree));

        stmt.destroy();
    }

    private void runAssertion3Stream1HistSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as ids0, s1.id as ids1, s2.id as ids2, h0.val as valh0 " +
                "from SupportBeanInt(id like 'S0%')#keepall as s0, " +
                "SupportBeanInt(id like 'S1%')#lastevent as s1, " +
                "SupportBeanInt(id like 'S2%')#lastevent as s2, " +
                "method:SupportJoinMethods.fetchVal(s1.id||s2.id||'H1', s0.p00) as h0 " +
                "order by s0.id, s1.id, s2.id, h0.val";

        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "ids0,ids1,ids2,valh0".split(",");
        sendBeanInt(epService, "S00", 2);
        sendBeanInt(epService, "S10", 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        assertFalse(listener.isInvoked());

        sendBeanInt(epService, "S20", 1);
        Object[][] resultOne = new Object[][]{{"S00", "S10", "S20", "S10S20H11"}, {"S00", "S10", "S20", "S10S20H12"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultOne);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "S01", 1);
        Object[][] resultTwo = new Object[][]{{"S01", "S10", "S20", "S10S20H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

        sendBeanInt(epService, "S21", 1);
        Object[][] resultThree = new Object[][]{{"S00", "S10", "S21", "S10S21H11"}, {"S00", "S10", "S21", "S10S21H12"}, {"S01", "S10", "S21", "S10S21H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultThree));

        stmt.destroy();
    }

    private void runAssertion3HistPureNoSubordinate(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");

        String expression;
        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H2', var3) as h2";
        tryAssertionFive(epService, expression);

        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
        tryAssertionFive(epService, expression);
    }

    private void tryAssertionFive(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "valh0,valh1,valh2".split(",");

        sendBeanInt(epService, "S00", 1, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H11", "H21"}});

        sendBeanInt(epService, "S01", 0, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S02", 1, 1, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S03", 1, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H11", "H21"}, {"H01", "H11", "H22"}});

        sendBeanInt(epService, "S04", 2, 2, 1);
        Object[][] result = new Object[][]{{"H01", "H11", "H21"}, {"H02", "H11", "H21"}, {"H01", "H12", "H21"}, {"H02", "H12", "H21"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion3Hist1Subordinate(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");

        String expression;
        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H2', var3) as h2";
        tryAssertionSix(epService, expression);

        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal('H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
        tryAssertionSix(epService, expression);
    }

    private void tryAssertionSix(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "valh0,valh1,valh2".split(",");

        sendBeanInt(epService, "S00", 1, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H11", "H01-H21"}});

        sendBeanInt(epService, "S01", 0, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S02", 1, 1, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S03", 1, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H11", "H01-H21"}, {"H01", "H11", "H01-H22"}});

        sendBeanInt(epService, "S04", 2, 2, 1);
        Object[][] result = new Object[][]{{"H01", "H11", "H01-H21"}, {"H02", "H11", "H02-H21"}, {"H01", "H12", "H01-H21"}, {"H02", "H12", "H02-H21"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion3Hist2SubordinateChain(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("on SupportBeanInt set var1=p00, var2=p01, var3=p02, var4=p03");

        String expression;
        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal(h1.val||'-H2', var3) as h2";
        tryAssertionSeven(epService, expression);

        expression = "select h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal(h1.val||'-H2', var3) as h2," +
                "method:SupportJoinMethods.fetchVal(h0.val||'-H1', var2) as h1," +
                "method:SupportJoinMethods.fetchVal('H0', var1) as h0";
        tryAssertionSeven(epService, expression);
    }

    private void tryAssertionSeven(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "valh0,valh1,valh2".split(",");

        sendBeanInt(epService, "S00", 1, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}});

        sendBeanInt(epService, "S01", 0, 1, 1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S02", 1, 1, 0);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "S03", 1, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}, {"H01", "H01-H11", "H01-H11-H22"}});

        sendBeanInt(epService, "S04", 2, 2, 1);
        Object[][] result = new Object[][]{{"H01", "H01-H11", "H01-H11-H21"}, {"H02", "H02-H11", "H02-H11-H21"}, {"H01", "H01-H12", "H01-H12-H21"}, {"H02", "H02-H12", "H02-H12-H21"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01, int p02, int p03) {
        epService.getEPRuntime().sendEvent(new SupportBeanInt(id, p00, p01, p02, p03, -1, -1));
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01, int p02) {
        sendBeanInt(epService, id, p00, p01, p02, -1);
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01) {
        sendBeanInt(epService, id, p00, p01, -1, -1);
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00) {
        sendBeanInt(epService, id, p00, -1, -1, -1);
    }

    public static ComputeCorrelationResult computeCorrelation(MySampleTradeEvent us, MySampleTradeEvent them) {
        return new ComputeCorrelationResult(us != null && them != null ? 1 : 0);
    }

    public static class ComputeCorrelationResult {
        private final int correlation;

        public ComputeCorrelationResult(int correlation) {
            this.correlation = correlation;
        }

        public int getCorrelation() {
            return correlation;
        }
    }

    public static class MySampleTradeEvent {
        private final String tradeId;
        private final String side;

        public MySampleTradeEvent(String tradeId, String side) {
            this.tradeId = tradeId;
            this.side = side;
        }

        public String getTradeId() {
            return tradeId;
        }

        public String getSide() {
            return side;
        }
    }
}
