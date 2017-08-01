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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanArrayCollMap;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecExprAnyAllSomeExpr implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);

        runAssertionEqualsAll(epService);
        runAssertionEqualsAllArray(epService);
        runAssertionEqualsAny(epService);
        runAssertionEqualsAnyBigInt(epService);
        runAssertionEqualsAnyArray(epService);
        runAssertionRelationalOpAllArray(epService);
        runAssertionRelationalOpNullOrNoRows(epService);
        runAssertionRelationalOpAnyArray(epService);
        runAssertionRelationalOpAll(epService);
        runAssertionRelationalOpAny(epService);
        runAssertionEqualsInNullOrNoRows(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionEqualsAnyBigInt(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        String stmtText = "select " +
                "bigInteger = any (null, 1) as c0," +
                "bigInteger = any (2, 3) as c1," +
                "bigDecimal = any (null, 1) as c2," +
                "bigDecimal = any (2, 3) as c3" +
                " from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean bean = new SupportBean();
        bean.setBigInteger(BigInteger.valueOf(1));
        bean.setBigDecimal(new BigDecimal(1d));
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, false, true, false});

        stmt.destroy();
    }

    private void runAssertionEqualsAll(EPServiceProvider epService) {
        String[] fields = "eq,neq,sqlneq,nneq".split(",");
        String stmtText = "select " +
                "intPrimitive=all(1,intBoxed) as eq, " +
                "intPrimitive!=all(1,intBoxed) as neq, " +
                "intPrimitive<>all(1,intBoxed) as sqlneq, " +
                "not intPrimitive=all(1,intBoxed) as nneq " +
                "from SupportBean(theString like \"E%\")";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // in the format intPrimitive, intBoxed
        int[][] testdata = {
                {1, 1},
                {1, 2},
                {2, 2},
                {2, 1},
        };

        Object[][] result = {
                {true, false, false, false}, // 1, 1
                {false, false, false, true}, // 1, 2
                {false, false, false, true}, // 2, 2
                {false, true, true, true}    // 2, 1
        };

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean("E", testdata[i][0]);
            bean.setIntBoxed(testdata[i][1]);
            epService.getEPRuntime().sendEvent(bean);
            //System.out.println("line " + i);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, result[i]);
        }

        // test OM
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText.replace("<>", "!="), model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean("E", testdata[i][0]);
            bean.setIntBoxed(testdata[i][1]);
            epService.getEPRuntime().sendEvent(bean);
            //System.out.println("line " + i);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, result[i]);
        }

        stmt.destroy();
    }

    private void runAssertionEqualsAllArray(EPServiceProvider epService) {
        String[] fields = "e,ne".split(",");
        String stmtText = "select " +
                "longBoxed = all ({1, 1}, intArr, longCol) as e, " +
                "longBoxed != all ({1, 1}, intArr, longCol) as ne " +
                "from ArrayBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
        arrayBean.setLongCol(Arrays.asList(1L, 1L));
        arrayBean.setLongBoxed(1L);
        epService.getEPRuntime().sendEvent(arrayBean);

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        arrayBean.setIntArr(new int[]{1, 1, 0});
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        arrayBean.setLongBoxed(2L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        stmt.destroy();
    }

    private void runAssertionEqualsAnyArray(EPServiceProvider epService) {
        String[] fields = "e,ne".split(",");
        String stmtText = "select " +
                "longBoxed = any ({1, 1}, intArr, longCol) as e, " +
                "longBoxed != any ({1, 1}, intArr, longCol) as ne " +
                "from ArrayBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
        arrayBean.setLongCol(Arrays.asList(1L, 1L));
        arrayBean.setLongBoxed(1L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        arrayBean.setIntArr(new int[]{1, 1, 0});
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        arrayBean.setLongBoxed(2L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        stmt.destroy();
    }

    private void runAssertionRelationalOpAllArray(EPServiceProvider epService) {
        String[] fields = "g,ge".split(",");
        String stmtText = "select " +
                "longBoxed>all({1,2},intArr,intCol) as g, " +
                "longBoxed>=all({1,2},intArr,intCol) as ge " +
                "from ArrayBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
        arrayBean.setIntCol(Arrays.asList(1, 2));
        arrayBean.setLongBoxed(3L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        arrayBean.setLongBoxed(2L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        arrayBean = new SupportBeanArrayCollMap(new int[]{1, 3});
        arrayBean.setIntCol(Arrays.asList(1, 2));
        arrayBean.setLongBoxed(3L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
        arrayBean.setIntCol(Arrays.asList(1, 3));
        arrayBean.setLongBoxed(3L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        // test OM
        stmt.destroy();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText.replace("<>", "!="), model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);

        arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
        arrayBean.setIntCol(Arrays.asList(1, 2));
        arrayBean.setLongBoxed(3L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        stmt.destroy();
    }

    private void runAssertionRelationalOpNullOrNoRows(EPServiceProvider epService) {
        // test array
        String[] fields = "vall,vany".split(",");
        String stmtText;
        EPStatement stmt;
        SupportUpdateListener listener = new SupportUpdateListener();

        stmtText = "select " +
                "intBoxed >= all ({doubleBoxed, longBoxed}) as vall, " +
                "intBoxed >= any ({doubleBoxed, longBoxed}) as vany " +
                " from SupportBean(theString like 'E%')";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEvent(epService, "E3", null, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
        sendEvent(epService, "E4", 1, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEvent(epService, "E5", null, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
        sendEvent(epService, "E6", 1, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, true});
        sendEvent(epService, "E7", 0, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        stmt.destroy();

        // test fields
        fields = "vall,vany".split(",");
        stmtText = "select " +
                "intBoxed >= all (doubleBoxed, longBoxed) as vall, " +
                "intBoxed >= any (doubleBoxed, longBoxed) as vany " +
                " from SupportBean(theString like 'E%')";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEvent(epService, "E3", null, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
        sendEvent(epService, "E4", 1, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEvent(epService, "E5", null, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});
        sendEvent(epService, "E6", 1, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, true});
        sendEvent(epService, "E7", 0, 1d, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        stmt.destroy();
    }

    private void runAssertionRelationalOpAnyArray(EPServiceProvider epService) {
        String[] fields = "g,ge".split(",");
        String stmtText = "select " +
                "longBoxed > any ({1, 2}, intArr, intCol) as g, " +
                "longBoxed >= any ({1, 2}, intArr, intCol) as ge " +
                "from ArrayBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(new int[]{1, 2});
        arrayBean.setIntCol(Arrays.asList(1, 2));
        arrayBean.setLongBoxed(1L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        arrayBean.setLongBoxed(2L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        arrayBean = new SupportBeanArrayCollMap(new int[]{2, 2});
        arrayBean.setIntCol(Arrays.asList(2, 1));
        arrayBean.setLongBoxed(1L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        arrayBean = new SupportBeanArrayCollMap(new int[]{1, 1});
        arrayBean.setIntCol(Arrays.asList(1, 1));
        arrayBean.setLongBoxed(0L);
        epService.getEPRuntime().sendEvent(arrayBean);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});

        stmt.destroy();
    }

    private void runAssertionEqualsAny(EPServiceProvider epService) {
        String[] fields = "eq,neq,sqlneq,nneq".split(",");
        String stmtText = "select " +
                "intPrimitive = any (1, intBoxed) as eq, " +
                "intPrimitive != any (1, intBoxed) as neq, " +
                "intPrimitive <> any (1, intBoxed) as sqlneq, " +
                "not intPrimitive = any (1, intBoxed) as nneq " +
                " from SupportBean(theString like 'E%')";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // in the format intPrimitive, intBoxed
        int[][] testdata = {
                {1, 1},
                {1, 2},
                {2, 2},
                {2, 1},
        };

        Object[][] result = {
                {true, false, false, false}, // 1, 1
                {true, true, true, false}, // 1, 2
                {true, true, true, false}, // 2, 2
                {false, true, true, true} // 2, 1
        };

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean("E", testdata[i][0]);
            bean.setIntBoxed(testdata[i][1]);
            epService.getEPRuntime().sendEvent(bean);
            //System.out.println("line " + i);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, result[i]);
        }

        stmt.destroy();
    }

    private void runAssertionRelationalOpAll(EPServiceProvider epService) {
        String[] fields = "g,ge,l,le".split(",");
        String stmtText = "select " +
                "intPrimitive > all (1, 3, 4) as g, " +
                "intPrimitive >= all (1, 3, 4) as ge, " +
                "intPrimitive < all (1, 3, 4) as l, " +
                "intPrimitive <= all (1, 3, 4) as le " +
                " from SupportBean(theString like 'E%')";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] result = {
                {false, false, true, true},
                {false, false, false, true},
                {false, false, false, false},
                {false, false, false, false},
                {false, true, false, false},
                {true, true, false, false}
        };

        for (int i = 0; i < 6; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", i));
            //System.out.println("line " + i);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, result[i]);
        }

        stmt.destroy();
    }

    private void runAssertionRelationalOpAny(EPServiceProvider epService) {
        String[] fields = "g,ge,l,le".split(",");
        String stmtText = "select " +
                "intPrimitive > any (1, 3, 4) as g, " +
                "intPrimitive >= some (1, 3, 4) as ge, " +
                "intPrimitive < any (1, 3, 4) as l, " +
                "intPrimitive <= some (1, 3, 4) as le " +
                " from SupportBean(theString like 'E%')";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object[][] result = {
                {false, false, true, true},
                {false, true, true, true},
                {true, true, true, true},
                {true, true, true, true},
                {true, true, false, true},
                {true, true, false, false}
        };

        for (int i = 0; i < 6; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E1", i));
            //System.out.println("line " + i);
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, result[i]);
        }

        stmt.destroy();
    }

    private void runAssertionEqualsInNullOrNoRows(EPServiceProvider epService) {
        // test fixed array case
        String[] fields;
        String stmtText;
        EPStatement stmt;
        SupportUpdateListener listener = new SupportUpdateListener();

        fields = "eall,eany,neall,neany,isin".split(",");
        stmtText = "select " +
                "intBoxed = all ({doubleBoxed, longBoxed}) as eall, " +
                "intBoxed = any ({doubleBoxed, longBoxed}) as eany, " +
                "intBoxed != all ({doubleBoxed, longBoxed}) as neall, " +
                "intBoxed != any ({doubleBoxed, longBoxed}) as neany, " +
                "intBoxed in ({doubleBoxed, longBoxed}) as isin " +
                " from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEvent(epService, "E3", null, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
        sendEvent(epService, "E4", 1, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        sendEvent(epService, "E5", null, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
        sendEvent(epService, "E6", 1, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, true, false, null, true});
        sendEvent(epService, "E7", 0, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, null, null, true, null});

        stmt.destroy();

        // test non-array case
        fields = "eall,eany,neall,neany,isin".split(",");
        stmtText = "select " +
                "intBoxed = all (doubleBoxed, longBoxed) as eall, " +
                "intBoxed = any (doubleBoxed, longBoxed) as eany, " +
                "intBoxed != all (doubleBoxed, longBoxed) as neall, " +
                "intBoxed != any (doubleBoxed, longBoxed) as neany, " +
                "intBoxed in (doubleBoxed, longBoxed) as isin " +
                " from SupportBean";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        sendEvent(epService, "E3", null, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
        sendEvent(epService, "E4", 1, null, null);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});

        sendEvent(epService, "E5", null, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null});
        sendEvent(epService, "E6", 1, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, true, false, null, true});
        sendEvent(epService, "E7", 0, null, 1L);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, null, null, true, null});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        try {
            String stmtText = "select intArr = all (1, 2, 3) as r1 from ArrayBean";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'intArr=all(1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr = all (1, 2, 3) as r1 from ArrayBean]", ex.getMessage());
        }

        try {
            String stmtText = "select intArr > all (1, 2, 3) as r1 from ArrayBean";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'intArr>all(1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr > all (1, 2, 3) as r1 from ArrayBean]", ex.getMessage());
        }
    }

    public void sendEvent(EPServiceProvider epService, String theString, Integer intBoxed, Double doubleBoxed, Long longBoxed) {
        SupportBean bean = new SupportBean(theString, -1);
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
