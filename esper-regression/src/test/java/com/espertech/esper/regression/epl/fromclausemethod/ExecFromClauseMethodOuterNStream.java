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
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanInt;
import com.espertech.esper.supportregression.epl.SupportJoinMethods;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertFalse;

public class ExecFromClauseMethodOuterNStream implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportBeanInt.class);
        configuration.addEventType(SupportBean.class);
        configuration.addImport(SupportJoinMethods.class.getName());
        configuration.addVariable("var1", Integer.class, 0);
        configuration.addVariable("var2", Integer.class, 0);
        configuration.addVariable("var3", Integer.class, 0);
        configuration.addVariable("var4", Integer.class, 0);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("on SupportBeanInt(id like 'V%') set var1=p00, var2=p01");

        runAssertion1Stream2HistStarSubordinateLeftRight(epService);
        runAssertion1Stream2HistStarSubordinateInner(epService);
        runAssertion1Stream2HistForwardSubordinate(epService);
        runAssertion1Stream3HistForwardSubordinate(epService);
        runAssertion1Stream3HistForwardSubordinateChain(epService);
        runAssertionInvalid(epService);
        runAssertion2Stream1HistStarSubordinateLeftRight(epService);
        runAssertion1Stream2HistStarNoSubordinateLeftRight(epService);
    }

    private void runAssertion1Stream2HistStarSubordinateLeftRight(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#keepall as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
        tryAssertionOne(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p03 = h1.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index" +
                " order by valh0, valh1";
        tryAssertionOne(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index " +
                " order by valh0, valh1";
        tryAssertionOne(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " full outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index" +
                " full outer join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index " +
                " order by valh0, valh1";
        tryAssertionOne(epService, expression);
    }

    private void runAssertion1Stream2HistStarSubordinateInner(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#keepall as s0 " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " on s0.p02 = h0.index " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
        tryAssertionTwo(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchValMultiRow('H0', p00, p04) as h0 " +
                " inner join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p02 = h0.index " +
                " inner join " +
                "method:SupportJoinMethods.fetchValMultiRow('H1', p01, p05) as h1 " +
                " on s0.p03 = h1.index" +
                " order by valh0, valh1";
        tryAssertionTwo(epService, expression);
    }

    private void runAssertion1Stream2HistForwardSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " on s0.p02 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " order by valh0, valh1";
        tryAssertionThree(epService, expression);
    }

    private void tryAssertionThree(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 0, 0, 1);
        Object[][] result = new Object[][]{{"E1", null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E2", 0, 1, 1);
        result = new Object[][]{{"E2", null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E3", 1, 0, 1);
        result = new Object[][]{{"E3", "H01", null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E4", 1, 1, 1);
        result = new Object[][]{{"E4", "H01", "H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E5", 4, 4, 2);
        result = new Object[][]{{"E5", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion1Stream3HistForwardSubordinate(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
        tryAssertionFour(epService, expression);

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 from " +
                "method:SupportJoinMethods.fetchVal('H0', p00) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " full outer join " +
                "method:SupportJoinMethods.fetchVal('H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
        tryAssertionFour(epService, expression);
    }

    private void tryAssertionFour(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1,valh2".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 0, 0, 0, 1);
        Object[][] result = new Object[][]{{"E1", null, null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E2", 0, 1, 1, 1);
        result = new Object[][]{{"E2", null, null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E3", 1, 1, 1, 1);
        result = new Object[][]{{"E3", "H01", "H11", "H21"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E4", 1, 0, 1, 1);
        result = new Object[][]{{"E4", "H01", null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E5", 4, 4, 4, 2);
        result = new Object[][]{{"E5", "H02", "H12", "H22"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void runAssertion1Stream3HistForwardSubordinateChain(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as id, h0.val as valh0, h1.val as valh1, h2.val as valh2 " +
                "from SupportBeanInt(id like 'E%')#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || '-H0', p00) as h0 " +
                " on s0.p03 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h0.val || '-H1', p01) as h1 " +
                " on h0.index = h1.index" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h1.val || '-H2', p02) as h2 " +
                " on h1.index = h2.index" +
                " order by valh0, valh1, valh2";
        tryAssertionFive(epService, expression);
    }

    private void tryAssertionFive(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1,valh2".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 0, 0, 0, 1);
        Object[][] result = new Object[][]{{"E1", null, null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E2", 0, 1, 1, 1);
        result = new Object[][]{{"E2", null, null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E3", 1, 1, 1, 1);
        result = new Object[][]{{"E3", "E3-H01", "E3-H01-H11", "E3-H01-H11-H21"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E4", 1, 0, 1, 1);
        result = new Object[][]{{"E4", "E4-H01", null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        sendBeanInt(epService, "E5", 4, 4, 4, 2);
        result = new Object[][]{{"E5", "E5-H02", "E5-H02-H12", "E5-H02-H12-H22"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, result);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, result);

        stmt.destroy();
    }

    private void tryAssertionOne(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 0, 0, 0, 0, 1, 1);
        Object[][] resultOne = new Object[][]{{"E1", null, null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultOne);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "E2", 1, 1, 1, 1, 1, 1);
        Object[][] resultTwo = new Object[][]{{"E2", "H01_0", "H11_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

        sendBeanInt(epService, "E3", 5, 5, 3, 4, 1, 1);
        Object[][] resultThree = new Object[][]{{"E3", "H03_0", "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree));

        sendBeanInt(epService, "E4", 0, 5, 3, 4, 1, 1);
        Object[][] resultFour = new Object[][]{{"E4", null, "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultFour);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour));

        sendBeanInt(epService, "E5", 2, 0, 2, 1, 1, 1);
        Object[][] resultFive = new Object[][]{{"E5", "H02_0", null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultFive);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive));

        // set 2 rows for H0
        sendBeanInt(epService, "E6", 2, 2, 2, 2, 2, 1);
        Object[][] resultSix = new Object[][]{{"E6", "H02_0", "H12_0"}, {"E6", "H02_1", "H12_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultSix);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive, resultSix));

        sendBeanInt(epService, "E7", 10, 10, 4, 5, 1, 2);
        Object[][] resultSeven = new Object[][]{{"E7", "H04_0", "H15_0"}, {"E7", "H04_0", "H15_1"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultSeven);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo, resultThree, resultFour, resultFive, resultSix, resultSeven));

        stmt.destroy();
    }

    private void tryAssertionTwo(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "id,valh0,valh1".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 0, 0, 0, 0, 1, 1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E2", 1, 1, 1, 1, 1, 1);
        Object[][] resultTwo = new Object[][]{{"E2", "H01_0", "H11_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo));

        sendBeanInt(epService, "E3", 5, 5, 3, 4, 1, 1);
        Object[][] resultThree = new Object[][]{{"E3", "H03_0", "H14_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        sendBeanInt(epService, "E4", 0, 5, 3, 4, 1, 1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        sendBeanInt(epService, "E5", 2, 0, 2, 1, 1, 1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree));

        // set 2 rows for H0
        sendBeanInt(epService, "E6", 2, 2, 2, 2, 2, 1);
        Object[][] resultSix = new Object[][]{{"E6", "H02_0", "H12_0"}, {"E6", "H02_1", "H12_0"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultSix);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree, resultSix));

        sendBeanInt(epService, "E7", 10, 10, 4, 5, 1, 2);
        Object[][] resultSeven = new Object[][]{{"E7", "H04_0", "H15_0"}, {"E7", "H04_0", "H15_1"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultSeven);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultTwo, resultThree, resultSix, resultSeven));

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String expression;
        // Invalid dependency order: a historical depends on it's own outer join child or descendant
        //              S0
        //      H0  (depends H1)
        //      H1
        expression = "select * from " +
                "SupportBeanInt#lastevent as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h1.val, 1) as h0 " +
                " on s0.p00 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 1) as h1 " +
                " on h0.index = h1.index";
        tryInvalid(epService, expression, "Error starting statement: Historical stream 1 parameter dependency originating in stream 2 cannot or may not be satisfied by the join [select * from SupportBeanInt#lastevent as s0  left outer join method:SupportJoinMethods.fetchVal(h1.val, 1) as h0  on s0.p00 = h0.index  left outer join method:SupportJoinMethods.fetchVal('H1', 1) as h1  on h0.index = h1.index]");

        // Optimization conflict : required streams are always executed before optional streams
        //              S0
        //  full outer join H0 to S0
        //  left outer join H1 to S0 (H1 depends on H0)
        expression = "select * from " +
                "SupportBeanInt#lastevent as s0 " +
                " full outer join " +
                "method:SupportJoinMethods.fetchVal('x', 1) as h0 " +
                " on s0.p00 = h0.index " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(h0.val, 1) as h1 " +
                " on s0.p00 = h1.index";
        tryInvalid(epService, expression, "Error starting statement: Historical stream 2 parameter dependency originating in stream 1 cannot or may not be satisfied by the join [select * from SupportBeanInt#lastevent as s0  full outer join method:SupportJoinMethods.fetchVal('x', 1) as h0  on s0.p00 = h0.index  left outer join method:SupportJoinMethods.fetchVal(h0.val, 1) as h1  on s0.p00 = h1.index]");
    }

    private void runAssertion2Stream1HistStarSubordinateLeftRight(EPServiceProvider epService) {
        String expression;

        //   S1 -> S0 -> H0 
        expression = "select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " on s0.p01 = h0.index " +
                " right outer join " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " on s1.p01 = s0.p01";
        tryAssertionSix(epService, expression);

        expression = "select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " left outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s1.p01 = s0.p01" +
                " left outer join " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " on s0.p01 = h0.index ";
        tryAssertionSix(epService, expression);

        expression = "select s0.id as s0id, s1.id as s1id, h0.val as valh0 from " +
                "method:SupportJoinMethods.fetchVal(s0.id || 'H0', s0.p00) as h0 " +
                " right outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p01 = h0.index " +
                " right outer join " +
                "SupportBeanInt(id like 'F%')#keepall as s1 " +
                " on s1.p01 = s0.p01";
        tryAssertionSix(epService, expression);
    }

    private void tryAssertionSix(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "s0id,s1id,valh0".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "E1", 1, 1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendBeanInt(epService, "F1", 1, 1);
        Object[][] resultOne = new Object[][]{{"E1", "F1", "E1H01"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultOne);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "F2", 2, 2);
        Object[][] resultTwo = new Object[][]{{null, "F2", null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultTwo));

        sendBeanInt(epService, "E2", 2, 2);
        Object[][] resultThree = new Object[][]{{"E2", "F2", "E2H02"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultThree);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree));

        sendBeanInt(epService, "F3", 3, 3);
        Object[][] resultFour = new Object[][]{{null, "F3", null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultFour);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree, resultFour));

        sendBeanInt(epService, "E3", 0, 3);
        Object[][] resultFive = new Object[][]{{"E3", "F3", null}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultFive);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, EPAssertionUtil.concatenateArray2Dim(resultOne, resultThree, resultFive));

        stmt.destroy();
    }

    private void runAssertion1Stream2HistStarNoSubordinateLeftRight(EPServiceProvider epService) {
        String expression;

        expression = "select s0.id as s0id, h0.val as valh0, h1.val as valh1 from " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 2) as h0 " +
                " on s0.p00 = h0.index " +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H1', 2) as h1 " +
                " on s0.p00 = h1.index";
        tryAssertionSeven(epService, expression);

        expression = "select s0.id as s0id, h0.val as valh0, h1.val as valh1 from " +
                "method:SupportJoinMethods.fetchVal('H1', 2) as h1 " +
                " left outer join " +
                "SupportBeanInt(id like 'E%')#keepall as s0 " +
                " on s0.p00 = h1.index" +
                " right outer join " +
                "method:SupportJoinMethods.fetchVal('H0', 2) as h0 " +
                " on s0.p00 = h0.index ";
        tryAssertionSeven(epService, expression);
    }

    private void tryAssertionSeven(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "s0id,valh0,valh1".split(",");
        Object[][] resultOne = new Object[][]{{null, "H01", null}, {null, "H02", null}, {null, null, "H11"}, {null, null, "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "E1", 0);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultOne);

        sendBeanInt(epService, "E2", 2);
        Object[][] resultTwo = new Object[][]{{"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        Object[][] resultIt = new Object[][]{{null, "H01", null}, {null, null, "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultIt);

        sendBeanInt(epService, "E3", 1);
        resultTwo = new Object[][]{{"E3", "H01", "H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        resultIt = new Object[][]{{"E3", "H01", "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultIt);

        sendBeanInt(epService, "E4", 1);
        resultTwo = new Object[][]{{"E4", "H01", "H11"}};
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, resultTwo);
        resultIt = new Object[][]{{"E3", "H01", "H11"}, {"E4", "H01", "H11"}, {"E2", "H02", "H12"}};
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, resultIt);

        stmt.destroy();
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01, int p02, int p03, int p04, int p05) {
        epService.getEPRuntime().sendEvent(new SupportBeanInt(id, p00, p01, p02, p03, p04, p05));
    }

    private void sendBeanInt(EPServiceProvider epService, String id, int p00, int p01, int p02, int p03) {
        sendBeanInt(epService, id, p00, p01, p02, p03, -1, -1);
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
}
