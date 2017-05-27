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
package com.espertech.esper.regression.nwtable.namedwindow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_B;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecNamedWindowOM implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionCompile(epService);
        runAssertionOM(epService);
        runAssertionOMCreateTableSyntax(epService);
    }

    private void runAssertionCompile(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};
        String stmtTextCreate = "create window MyWindow#keepall as select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelCreate = epService.getEPAdministrator().compileEPL(stmtTextCreate);
        EPStatement stmtCreate = epService.getEPAdministrator().create(modelCreate);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);
        assertEquals("create window MyWindow#keepall as select theString as key, longBoxed as value from " + SupportBean.class.getName(), modelCreate.toEPL());

        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " select mywin.* from MyWindow as mywin";
        EPStatementObjectModel modelOnSelect = epService.getEPAdministrator().compileEPL(stmtTextOnSelect);
        EPStatement stmtOnSelect = epService.getEPAdministrator().create(modelOnSelect);
        SupportUpdateListener listenerOnSelect = new SupportUpdateListener();
        stmtOnSelect.addListener(listenerOnSelect);

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEPL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow(key is not null)";
        EPStatementObjectModel modelSelect = epService.getEPAdministrator().compileEPL(stmtTextSelectOne);
        EPStatement stmtSelectOne = epService.getEPAdministrator().create(modelSelect);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        assertEquals(stmtTextSelectOne, modelSelect.toEPL());

        // send events
        sendSupportBean(epService, "E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean(epService, "E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        // create delete stmt
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
        EPStatementObjectModel modelDelete = epService.getEPAdministrator().compileEPL(stmtTextDelete);
        epService.getEPAdministrator().create(modelDelete);
        assertEquals("on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol=s1.key", modelDelete.toEPL());

        // send delete event
        sendMarketBean(epService, "E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean(epService, "E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

        // trigger on-select on empty window
        assertFalse(listenerOnSelect.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        sendSupportBean(epService, "E3", 30L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E3", 60L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        // trigger on-select on the filled window
        epService.getEPRuntime().sendEvent(new SupportBean_B("B2"));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    private void runAssertionOM(EPServiceProvider epService) {
        String[] fields = new String[]{"key", "value"};

        // create window object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setCreateWindow(CreateWindowClause.create("MyWindow").addView("keepall"));
        model.setSelectClause(SelectClause.create()
                .addWithAsProvidedName("theString", "key")
                .addWithAsProvidedName("longBoxed", "value"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));

        EPStatement stmtCreate = epService.getEPAdministrator().create(model);
        SupportUpdateListener listenerWindow = new SupportUpdateListener();
        stmtCreate.addListener(listenerWindow);

        String stmtTextCreate = "create window MyWindow#keepall as select theString as key, longBoxed as value from " + SupportBean.class.getName();
        assertEquals(stmtTextCreate, model.toEPL());

        String stmtTextInsert = "insert into MyWindow select theString as key, longBoxed as value from " + SupportBean.class.getName();
        EPStatementObjectModel modelInsert = epService.getEPAdministrator().compileEPL(stmtTextInsert);
        EPStatement stmtInsert = epService.getEPAdministrator().create(modelInsert);

        // Consumer statement object model
        model = new EPStatementObjectModel();
        Expression multi = Expressions.multiply(Expressions.property("value"), Expressions.constant(2));
        model.setSelectClause(SelectClause.create().streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH)
                .add("key")
                .add(multi, "value"));
        model.setFromClause(FromClause.create(FilterStream.create("MyWindow", Expressions.isNotNull("value"))));

        EPStatement stmtSelectOne = epService.getEPAdministrator().create(model);
        SupportUpdateListener listenerStmtOne = new SupportUpdateListener();
        stmtSelectOne.addListener(listenerStmtOne);
        String stmtTextSelectOne = "select irstream key, value*2 as value from MyWindow(value is not null)";
        assertEquals(stmtTextSelectOne, model.toEPL());

        // send events
        sendSupportBean(epService, "E1", 10L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10L});

        sendSupportBean(epService, "E2", 20L);
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetNewAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 20L});

        // create delete stmt
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnDelete("MyWindow", "s1"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportMarketDataBean.class.getName(), "s0")));
        model.setWhereClause(Expressions.eqProperty("s0.symbol", "s1.key"));
        epService.getEPAdministrator().create(model);
        String stmtTextDelete = "on " + SupportMarketDataBean.class.getName() + " as s0 delete from MyWindow as s1 where s0.symbol=s1.key";
        assertEquals(stmtTextDelete, model.toEPL());

        // send delete event
        sendMarketBean(epService, "E1");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E1", 20L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 10L});

        // send delete event again, none deleted now
        sendMarketBean(epService, "E1");
        assertFalse(listenerStmtOne.isInvoked());
        assertFalse(listenerWindow.isInvoked());

        // send delete event
        sendMarketBean(epService, "E2");
        EPAssertionUtil.assertProps(listenerStmtOne.assertOneGetOldAndReset(), fields, new Object[]{"E2", 40L});
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 20L});

        // On-select object model
        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnSelect("MyWindow", "s1"));
        model.setWhereClause(Expressions.eqProperty("s0.id", "s1.key"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean_B.class.getName(), "s0")));
        model.setSelectClause(SelectClause.createStreamWildcard("s1"));
        EPStatement statement = epService.getEPAdministrator().create(model);
        SupportUpdateListener listenerOnSelect = new SupportUpdateListener();
        statement.addListener(listenerOnSelect);
        String stmtTextOnSelect = "on " + SupportBean_B.class.getName() + " as s0 select s1.* from MyWindow as s1 where s0.id=s1.key";
        assertEquals(stmtTextOnSelect, model.toEPL());

        // send some more events
        sendSupportBean(epService, "E3", 30L);
        sendSupportBean(epService, "E4", 40L);

        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        assertFalse(listenerOnSelect.isInvoked());

        // trigger on-select
        epService.getEPRuntime().sendEvent(new SupportBean_B("E3"));
        EPAssertionUtil.assertProps(listenerOnSelect.assertOneGetNewAndReset(), fields, new Object[]{"E3", 30L});

        stmtSelectOne.destroy();
        stmtInsert.destroy();
        stmtCreate.destroy();
    }

    private void runAssertionOMCreateTableSyntax(EPServiceProvider epService) {
        String expected = "create window MyWindowOM#keepall as (a1 string, a2 double, a3 int)";

        // create window object model
        EPStatementObjectModel model = new EPStatementObjectModel();
        CreateWindowClause clause = CreateWindowClause.create("MyWindowOM").addView("keepall");
        clause.addColumn(new SchemaColumnDesc("a1", "string", false, false));
        clause.addColumn(new SchemaColumnDesc("a2", "double", false, false));
        clause.addColumn(new SchemaColumnDesc("a3", "int", false, false));
        model.setCreateWindow(clause);
        assertEquals(expected, model.toEPL());

        EPStatement stmtCreate = epService.getEPAdministrator().create(model);
        assertEquals(expected, stmtCreate.getText());
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, Long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendMarketBean(EPServiceProvider epService, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, 0L, "");
        epService.getEPRuntime().sendEvent(bean);
    }
}
